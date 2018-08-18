package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
 * Splits a file and creates a preprocessing thread for each file, then merges them back
 */
@Slf4j
public class ParallelPreProcessor {
//    private File[] tempFiles;
//    private File input;

//    private long totalLines, linesPerFile;
    private int threadCount; // also file count

    public ParallelPreProcessor(File input) throws IOException {
//        this.input = input;
        this.threadCount = Runtime.getRuntime().availableProcessors();
//        totalLines = countLines(input);
//        linesPerFile = totalLines / threadCount;
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

    // transfers
    private static final void efficientTransfer(final Reader source, final Writer destination) throws IOException {
        char[] buffer = new char[1024 * 16];
        int len = 0;
        while ((len = source.read(buffer)) >= 0) {
            destination.write(buffer, 0, len);
        }
    }

    public void processFile(File input, File output) throws IOException {
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

        System.out.println("total lines = " + totalLines);
        System.out.println("lines per file = " + linesPerFile);

        // create threads for each file
        List<PreProcessorThread> threads = new ArrayList<>();
        List<Path> outputTemps = new ArrayList<>();

        for(int j = 0; j < tempFiles.size(); j++) {
            Path outTemp = Files.createTempFile(tempDir, j + "tempfile", ".processed");
            outputTemps.add(outTemp);
            PreProcessorThread thread = new PreProcessorThread(j + 1, tempFiles.get(j).toString(), outTemp);
            thread.start();
            threads.add(thread);
        }

        for(int x = 0; x < threads.size(); x++) {
            try {
                threads.get(x).join();
            } catch(InterruptedException e) {
                log.error("Thread {} was interrupted", threads.get(x).getName());
            }
        }

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
    }

    private class PreProcessorThread extends Thread {
        private final int threadId;
        private final PreProcessor pp;
        private final String input;
        private final Path output;

        public PreProcessorThread(int threadId, String input, Path output) throws IOException {
            this.threadId = threadId;
            this.pp = new PreProcessor();
            this.input = input;
            this.output = output;

            this.setName("PreProcessorThread " + threadId);
        }

        public void run() {
            pp.processFile(input, output, false);
        }
    }

    public static void main(String[] args) throws IOException {
        ParallelPreProcessor ppp = new ParallelPreProcessor(
                Paths.get("data\\corpora\\short_corpus.txt").toFile());
        ppp.processFile(new File("data\\corpora\\short_corpus.txt"), new File("data\\corpora\\short_corpus.processed"));
    }
}
