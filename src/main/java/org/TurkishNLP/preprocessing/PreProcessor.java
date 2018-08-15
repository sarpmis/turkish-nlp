package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.shared.Timer;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.core.turkish.PrimaryPos;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class PreProcessor {

    private TurkishMorphology morphology;
    private TurkishSentenceExtractor extractor;

    public PreProcessor() throws IOException {
        morphology = TurkishMorphology.createWithDefaults();
        extractor = TurkishSentenceExtractor.DEFAULT;
    }


    public List<DictionaryItem> analyzeSentence(String s) {
        List<DictionaryItem> lst = new ArrayList<>();

        List<WordAnalysis> analyses = morphology.analyzeSentence(s);

        // Can't include words that zemberek can't find in the dictionary
        // or else disambiguate breaks
        // THIS SEEMS TO BE FIXED IN 0.15
        // analyses.removeIf(a -> a.analysisCount() == 0);

        SentenceAnalysis result = morphology.disambiguate(s, analyses);

        // Build a list from the output of disambiguate.
        for (SingleAnalysis sa : result.bestAnalysis()) {
            lst.add(sa.getDictionaryItem());
        }
        return lst;
    }

    /*
     * @return long[] - array of two elements. [0] is sentenceCount
     *      [1] is tokenCount
     *
     */
    public long[] analyzeAndWrite(Scanner sc, PrintWriter writer, boolean sideBySideMode) {
        long sentenceCount = 0, tokenCount = 0;
        String currentLine;
        while(sc.hasNextLine()){
            currentLine = sc.nextLine();

            // Extract sentences from current line.
            // wiki dumps do not have sentences that wrap onto different lines.
            // but they do have multiple sentences in a single line
            // TODO: double check this ^ and make wrapped sentences work as well
            List<String> sentences = extractor.fromParagraph(currentLine);

            for(String s : sentences){
                if(sideBySideMode) writer.print(s + " #");
                for(DictionaryItem item : analyzeSentence(s)){
                    if(!item.primaryPos.equals(PrimaryPos.Punctuation)) {
                        writer.print(" " + item.getId());
                        tokenCount++;
                    }
                }
                writer.print(System.lineSeparator());
                sentenceCount++;
            }
        }
        writer.close();
        sc.close();
        return new long[] {sentenceCount, tokenCount};
    }

    // TODO: Make this throw IOEXception instead of a billion try catch statements!!
    public void processFile(String filepath, boolean inMem) {
        Path outPath = Paths.get(System.getProperty("user.dir"), "data", "processed_files",
                Paths.get(filepath).getFileName().toString().split("\\.")[0] + ".processed2");
        if (outPath.toFile().exists()) {
            log.info("A processed file for " + filepath + " already exists, aborting...");
            return;
        }

        log.info("Starting processing file " + filepath);

        Timer.setTimer();
        String cleaned = "";
        Path tempPath = null;
        if(inMem) {
            log.info("Cleaning the text in memory...");
            cleaned = Cleaner.cleanFileInMemory(filepath);
            if (cleaned == null) return;
        } else {
            log.info("Cleaning the text on disk...");
            tempPath = Cleaner.cleanFileOnDisk(filepath);
            if (tempPath == null) return;
        }

        log.info("Completed cleaning text...");
        log.info("Beginning analysis...");

        PrintWriter pw;
        try {
            // Use a buffer to avoid using too much mem. use 10mb
            pw = new PrintWriter(outPath.toFile());
        } catch(IOException e) {
            log.error("This should never happen but we must please the Java god");
            return;
        }

        // Scanner to read cleaned corpus line by line
        Scanner sc = null;
        if(inMem){
            sc = new Scanner(cleaned);
        } else {
            try {
                sc = new Scanner(tempPath.toFile());
            } catch (IOException e) {
                log.error("This should never happen but we must please the Java god");
                return;
            }
        }

        long[] analysisCounts = analyzeAndWrite(sc, pw, false);

        // If we did this on disk we should clean up after ourselves
        if(!inMem) try {
            Files.delete(tempPath);
        } catch(IOException e) {
            log.error("This should never happen but we must please the Java god");
            return;
        }

        Timer.endTimer();
        log.info("Finished processing file " + filepath);
        log.info("Processed {} sentences and {} tokens in " + Timer.results(), analysisCounts[0], analysisCounts[1]);
    }

    // Regex to match any token that contains numbers followed by _Num
    public String removeProcessedNumbers(String str) {
        return str.replaceAll("[\\s\\n]?\\pN*\\.?\\pN*_Num\\S*\\s", " " + DictionaryItem.UNKNOWN.getId() + " ");
    }

    public void removeProcessedNumbersOnDisk(Path filepath, Path outpath) throws IOException {
        Scanner sc = new Scanner(filepath);
        PrintWriter pw = new PrintWriter(outpath.toFile());
        String line;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            pw.println(removeProcessedNumbers(line) );
        }
    }

    // ****************************** USED FOR TESTING ****************************** \\
    public static void main ( String[] args) throws IOException {
//        System.out.println(pp.analyzeSentence("Nevşehir'deki mitingin ardından Adıyaman'a geçerek yurttaşlarla bir araya geldi."));
//        PreProcessor pp = new PreProcessor();
//        pp.processFile("data\\corpora\\trwiki_corpus.txt", false);
    }
}
