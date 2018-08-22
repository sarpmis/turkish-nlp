package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.shared.Timer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Class used to preprocess files in parallel. Multiple Preprocessors are created and asynchronous
 * buffers are used to read and write to file. Order of lines in file is preserved.
 * Buffers take in number of lines to hold so if input file contains *HUGE* lines buffer size should
 * be small
 */
@Slf4j
public class Parallel<T extends ParallelizablePreProcessor> {
    private static final long LOG_PROGRESS_FREQ = 10000;

    private final Class<T> cls;
    private final int workers;
    private AtomicLong lineCount;
    private long totalLines;

    /*
     * @param cls
     *      the class of T should be provided to create new instances of it
     *      ex. new Parallel<TextCleaner>(TextCleaner.class)
     */
    public Parallel(Class<T> cls){
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

    public boolean processFile(File input, File output) {
        try(
            Scanner in = new Scanner(input);
            PrintWriter out = new PrintWriter(output);
            )
        {
            Timer.setTimer();
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
                        new PreProcessorThread<>(i+1, readBuffer, writeBuffer, cls, lineCount);
                pp.start();
                threads.add(pp);
            }

            for(PreProcessorThread<T> thread : threads) {
                try {
                    thread.join();
                } catch(InterruptedException e) {
                    log.error("Thread {} interrupted!", thread.getName());
                    Thread.currentThread().interrupt();
                }
            }

            try {
                readBuffer.join();
                writeBuffer.finish();
                writeBuffer.join();
            } catch(InterruptedException e) {
                // this shouldn't happen
                log.error("Buffer thread interrupted!");
                Thread.currentThread().interrupt();
            }
            Timer.endTimer();
            log.info("Finished processing file in " + Timer.results());
        } catch(FileNotFoundException e) {
            log.error("Error processing. File not found...");
            return false;
        }
        return true;
    }

    protected class AsyncReadBuffer extends Thread {
        private AtomicBoolean isRunning = new AtomicBoolean(true);
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
            while(in.hasNextLine()) {
                if(buffer.size() < bufferMin) {
                    String line = in.nextLine();
                    try {
                        buffer.put(new LongAndString(lineCountRead.getAndIncrement(), line));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
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
     */
    protected class AsyncWriteBuffer extends Thread {
        private AtomicBoolean isRunning = new AtomicBoolean(true);
        private ConcurrentHashMap<Long, String> buffer;
        private final PrintWriter out;
        private long index = 0;
        private int softCap;

        public AsyncWriteBuffer(PrintWriter out, int softCap) {
            buffer = new ConcurrentHashMap<>();
            this.out = out;
            this.softCap = softCap;
        }

        public void addToBuffer(Long index, String s) {
            buffer.put(index, s);
        }

        /*
         * Starting from index, writes as many lines from the buffer as possible
         */
        public void flush() {
            Long key = new Long(index);
            while(buffer.containsKey(key)) {
                out.println(buffer.get(key));
                buffer.remove(key);
                index++;
                key = new Long(index);
            }
        }

        public void finish() {
            isRunning.set(false);
        }

        @Override
        public void run() {
            while(isRunning.get()) {
                int size = buffer.size();
                if(size >= softCap) flush();
            }
            flush();
        }
    }

    private class PreProcessorThread<K extends ParallelizablePreProcessor> extends Thread {
        private final int threadId;
        private final K pp;
        private final AsyncReadBuffer input;
        private final AsyncWriteBuffer output;

        public PreProcessorThread(int threadId,
                                  AsyncReadBuffer input,
                                  AsyncWriteBuffer output,
                                  Class<K> cls,
                                  AtomicLong lineCount)  {
            this.threadId = threadId;
            this.input = input;
            this.output = output;
            this.setName("PreProcessorThread " + threadId);
            try {
                this.pp = cls.newInstance();
            } catch (Exception e) {
                log.error("Cannot instantiate class [{}]. " +
                        "Check access to class and make sure it has an empty constructor", cls.getName());
                throw new RuntimeException();
            }
        }

        @Override
        public void run() {
            log.info("Starting process...");
            while(input.hasMoreLines()) {
                LongAndString line = input.nextLine();
                if(line == null) continue;
                String processed = pp.processLine(line.getString());
                output.addToBuffer(line.getLong(), processed);
                long lines = lineCount.incrementAndGet();
                if(lines % LOG_PROGRESS_FREQ == 0) {
                    double percentage = Math.round(((lines * 100.0d) / totalLines)*100.0) / 100.0;
                    log.info("Processed [{}] lines so far: [{}%]", lines, percentage);
                }
            }
            log.info("Completed process.");
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
