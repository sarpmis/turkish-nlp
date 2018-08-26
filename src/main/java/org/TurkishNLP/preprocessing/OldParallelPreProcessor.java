package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.shared.Timer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/*
 * Naive implementation for parallelizing  preprocessors
 * Splits a file and creates a preprocessing thread for each file, then merges them back
 * Might not be a good idea if system has HDD but still effective
 * FIXME: fix old parallel
 */
@Slf4j
public class OldParallelPreProcessor<T extends PreProcessor> {
    private int threadCount;

    public OldParallelPreProcessor() throws IOException {
        this.threadCount = Runtime.getRuntime().availableProcessors();
    }

    private long countLines(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        long count = 0;
        while(br.readLine() != null) {
            count++;
        }
        return count;
    }

    /*
     * writes the next lineCount lines in br to out
     */
    private void writeLines(long lineCount, BufferedReader br, PrintWriter out) throws IOException {
        long linesWritten = 0;
        String line = br.readLine();
        while(line != null) {
            out.println(line);
            linesWritten++;
            if(linesWritten == lineCount) break;
            line = br.readLine();
        }
        out.flush();
    }

    /*
     * Reads from source and Writes to destination using a character buffer
     */
    private static final void efficientTransfer(Reader source, Writer destination) throws IOException {
        char[] buffer = new char[1024 * 16];
        int len;
        while ((len = source.read(buffer)) >= 0) {
            destination.write(buffer, 0, len);
        }
    }

    public void processFile(File input, File output) throws IOException {
        log.info("Starting processing file " + input + " in parallel...");

        Timer.TimerToken timerToken = Timer.newToken();

        // create temp directory
        Path tempDir = Files.createTempDirectory(Paths.get(System.getProperty("user.dir"), "data"), "temp");

        long totalLines = countLines(input);
        long linesPerFile = totalLines / threadCount;

        // create temp files in directory
        List<Path> tempFiles = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (int i = 0; i < threadCount; i++) {
                Path temp = Files.createTempFile(tempDir, i + "tempfile", ".txt");
                PrintWriter printy = new PrintWriter(temp.toFile());
                writeLines(linesPerFile, br, printy);
                // if it is the last file and there are lines remaining we add them
                if (i == threadCount - 1 && br.ready()) writeLines(linesPerFile, br, printy);
                printy.close();
                tempFiles.add(temp);
            }
        }

        log.info("Input file contains {} lines", totalLines);

        // create threads for each file
        List<PreProcessorThread> threads = new ArrayList<>();
        List<Path> outputTemps = new ArrayList<>();

        for(int j = 0; j < tempFiles.size(); j++) {
            Path outTemp = Files.createTempFile(tempDir, j + "tempfile", ".processed");
            outputTemps.add(outTemp);
//            PreProcessorThread thread = new PreProcessorThread(j + 1, tempFiles.get(j).toString(), outTemp);
//            thread.start();
//            threads.add(thread);
        }

        for(int x = 0; x < threads.size(); x++) {
            try {
                threads.get(x).join();
            } catch(InterruptedException e) {
                log.error("Thread {} was interrupted", threads.get(x).getName());
            }
        }

        log.info("Saving to final destination " + output);

        // merge files into one
        try(PrintWriter out = new PrintWriter(output)) {
            for (Path temp : outputTemps) {
                try (BufferedReader tempReader = Files.newBufferedReader(temp)) {
                    efficientTransfer(tempReader, out);
                }
            }
        }

        // delete temp files
        for(Path path : tempFiles) {
            Files.delete(path);
        }
        for(Path path : outputTemps) {
            Files.delete(path);
        }
        Files.delete(tempDir);

        Timer.TimerResults res = Timer.checkOut(timerToken);
        log.info("Successfully finished processing in " + res.humanReadableIncludeMillis());
    }

    private class PreProcessorThread<T> extends Thread {
        private final int threadId;
//        private final TurkishLemmatizer pp;
        private final String input;
        private final String output;

        public PreProcessorThread(int threadId, String input, String output)  {
            this.threadId = threadId;
//            this.pp = new TurkishLemmatizer();
            this.input = input;
            this.output = output;

            this.setName("PreProcessorThread " + threadId);
        }

        @Override
        public void run() {
            try {
//                pp.processFile(input, output);
            } catch(Exception e) {
                System.out.println("Exception in " + this.getName());
            }
        }
    }
}
