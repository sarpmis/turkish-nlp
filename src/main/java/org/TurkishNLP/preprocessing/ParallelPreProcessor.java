package org.TurkishNLP.preprocessing;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.preprocessing.impl.TextCleaner;
import org.TurkishNLP.preprocessing.impl.TurkishLemmatizer;
import org.TurkishNLP.shared.Timer;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/*
 * Class used to process files in parallel. Multiple Preprocessors are created and asynchronous
 * buffers are used to read and write to file to/from files. Order of lines in the input file is preserved in output.
 * Buffers use String objects instead of byte arrays (since lines can be of variable length) so if input file
 * contains *HUGE* lines buffer size should be small
 * TODO: using a bytebuffer sounds better
 */
@Slf4j
public class ParallelPreProcessor<T extends ParallelizablePreProcessor> {
    private static final long LOG_PROGRESS_FREQ = 10000;

    private final Class<T> cls;
    private final int workers;
    private AtomicLong lineCount;
    private long totalLines;

    /*
     * @param cls
     *      the class of T should be provided to create new instances of it
     *      ex. new ParallelPreProcessor<TextCleaner>(TextCleaner.class)
     */
    public ParallelPreProcessor(Class<T> cls){
        this.cls = cls;
        workers = Runtime.getRuntime().availableProcessors();
        lineCount = new AtomicLong(0);
    }

    public T getInstanceOfT() throws InstantiationException, IllegalAccessException {
        return cls.newInstance();
    }

    private long countLines(Scanner in) {
        long count = 0;
        while(in.hasNextLine()) {
            in.nextLine();
            count++;
        }
        return count;
    }

    /*
     * @return processing succeeded
     */
    public boolean processFile(File input, File output) {
        try(
            Scanner in = new Scanner(input);
            PrintWriter out = new PrintWriter(output);
            )
        {
            Timer.TimerToken timerToken = Timer.newToken();
            log.info("Counting lines in file...");
            totalLines = countLines(new Scanner(input));
            in.reset();
            log.info("Processing file [{}], a total of [{}] lines, in parallel using [{}] threads...",
                    input, totalLines, workers);
            AsyncReadBuffer readBuffer = new AsyncReadBuffer(in, 100, 300, new AtomicLong(0));
            AsyncWriteBuffer writeBuffer = new AsyncWriteBuffer(out, 100);

            readBuffer.start();
            writeBuffer.start();

            List<PreProcessorThread<T>> threads = new ArrayList<>();

            for(int i = 0; i < workers; i++) {
                PreProcessorThread<T> pp =
                        new PreProcessorThread<>(i+1, readBuffer, writeBuffer, lineCount, cls);
                pp.start();
                threads.add(pp);
            }

            for(PreProcessorThread<T> thread : threads) {
                try {
                    thread.join();
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            try {
                readBuffer.join();
                writeBuffer.finish();
                writeBuffer.join();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Timer.TimerResults res = Timer.checkOut(timerToken);
            log.info("Successfully finished processing in " + res.humanReadableIncludeMillis());
        } catch(FileNotFoundException | RuntimeException e) {
            if(e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            else if(e instanceof FileNotFoundException)
                log.error("Error processing. File not found: " + ((FileNotFoundException) e).getMessage());
            return false;
        }
        return true;
    }

    public boolean processFile(String inputPath, String outputPath) {
        return this.processFile(new File(inputPath), new File(outputPath));
    }

    public boolean processFile(Path inputPath, Path outputPath) {
        return this.processFile(inputPath.toFile(), outputPath.toFile());
    }

    protected class AsyncReadBuffer extends Thread {
        private AtomicBoolean isRunning = new AtomicBoolean(false);
        private final LinkedBlockingQueue<LongAndString> buffer;
        private final Scanner in;
        private final int bufferMin, bufferMax;
        private AtomicLong lineCountAway, lineCountRead;

        public AsyncReadBuffer(Scanner in,
                               int bufferMin,
                               int bufferMax,
                               AtomicLong lineCountAway) {
            this.buffer = new LinkedBlockingQueue<>();
            this.in = in;
            this.bufferMin = bufferMin;
            this.bufferMax = bufferMax;
            this.lineCountAway = lineCountAway;
            lineCountRead = new AtomicLong(0);
        }

        @Override
        public void run() {
            isRunning.set(true);
            while(in.hasNextLine()) {
                if(buffer.size() < bufferMin) {
                    // fill buffer
                    while(buffer.size() < bufferMax) {
                        if(!in.hasNextLine()) break;
                        String line = in.nextLine();
                        try {
                            buffer.put(new LongAndString(lineCountRead.getAndIncrement(), line));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            isRunning.set(false);
        }

        public synchronized boolean hasMoreLines(){
            return isRunning.get() || !buffer.isEmpty();
        }

        public synchronized LongAndString nextLine() {
            try {
                LongAndString ls = buffer.poll(5L, TimeUnit.SECONDS);
                lineCountAway.incrementAndGet();
                return ls;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    /*
     * Stores lines and their line numbers in a hash and writes them (while keeping order)
     * as it reaches its capacity.
     *
     * If a null String is added to the hash map nullToken, which is (practically always) a unique id for this
     * object is used.
     */
    protected class AsyncWriteBuffer extends Thread {
        private AtomicBoolean isRunning = new AtomicBoolean(false);
        private ConcurrentHashMap<Long, String> buffer;
        private final PrintWriter out;
        private long index = 0;
        private int softCap;
        private final String nullToken;

        private static final int TIMEOUT = 10000;

        public AsyncWriteBuffer(PrintWriter out, int softCap) {
            buffer = new ConcurrentHashMap<>();
            this.out = out;
            this.softCap = softCap;
            nullToken = "NULL_LINE" + System.identityHashCode(this);
        }

        public void addToBuffer(@NonNull Long index, @Nullable String s) {
            if(s == null) {
                buffer.put(index, nullToken);
            } else {
                buffer.put(index, s);
            }
        }

        /*
         * Starting from index, writes as many lines from the buffer as possible
         */
        public void flush() {
            Long key = new Long(index);
            while(buffer.containsKey(key)) {
                String val = buffer.get(key);
                if(!val.equals(nullToken)) out.println(val);
                buffer.remove(key);
                key = new Long(index++);
            }
        }

        public void finish() {
            isRunning.set(false);
        }

        @Override
        public void run() {
            isRunning.set(true);
            while(isRunning.get()) {
                if(buffer.size() >= softCap) flush();
            }
            long startTime = System.currentTimeMillis();
            while(!buffer.isEmpty()) {
                flush();
                // an infite loop can happen here if for some reason a line number is not added to
                // the buffer by a preprocessor thread (maybe it can happen if the thread is interrupted or the
                // read buffer fails to read the whole file due to some IO issue). the buffer index would be stuck
                // at that number forever, since not writing that line compromises correctness this deserves
                // a program crash.
                if(System.currentTimeMillis() - startTime > TIMEOUT) {
                    throw new RuntimeException("Concurrency error! Things went really wrong!");
                }
            }
        }
    }

    private class PreProcessorThread<K extends ParallelizablePreProcessor> extends Thread {
        private final int threadId;
        private final K worker;
        private final AsyncReadBuffer input;
        private final AsyncWriteBuffer output;
        private final AtomicLong lineCount;

        PreProcessorThread(int threadId,
                                  AsyncReadBuffer input,
                                  AsyncWriteBuffer output,
                                  AtomicLong lineCount,
                                  Class<K> cls)  {
            this.threadId = threadId;
            this.setName(cls.getSimpleName() + " Thread " + threadId);
            this.input = input;
            this.output = output;
            this.lineCount = lineCount;
            try {
                this.worker = cls.newInstance();
            } catch (Exception e) {
                log.error("Cannot instantiate class [{}]. " +
                        "Check access to class and make sure it has an empty constructor!", cls);
                throw new RuntimeException(this.getName());
            }
        }

        @Override
        public void run() {
            while(input.hasMoreLines()) {
                LongAndString line = input.nextLine();
                if(line == null) continue;
                String processed = worker.processLine(line.getString());
                output.addToBuffer(line.getLong(), processed);
                long lines = lineCount.incrementAndGet();
                if(lines % LOG_PROGRESS_FREQ == 0 || lines == totalLines) {
                    double percentage = Math.round(((lines * 100.0d) / totalLines)*100.0) / 100.0;
                    log.info("Processed [{}] lines so far: [{}%]", lines, percentage);
                }
            }
        }
    }

    // This is a sad, sad class
    private class LongAndString {
        private long l;
        private String s;

        public LongAndString(long l, String s) {
            this.l = l;
            this.s = s;
        }

        public long getLong() {
            return l;
        }

        public String getString() {
            return s;
        }
    }
}
