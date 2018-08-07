package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.shared.Timer;
import org.TurkishNLP.shared.WriteBuffer;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.core.turkish.PrimaryPos;

import java.io.File;
import java.io.IOException;
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

    public long[] analyzeAndWrite(Scanner sc, WriteBuffer buffer) {
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
                buffer.add(s + " #");
                for(DictionaryItem item : analyzeSentence(s)){
                    if(!item.primaryPos.equals(PrimaryPos.Punctuation)) {
                        buffer.add(" " + item.getId());
                        tokenCount++;
                    }
                }
                buffer.add(System.lineSeparator());
                sentenceCount++;
            }
        }
        buffer.finish();
        sc.close();
        return new long[] {sentenceCount, tokenCount};
    }

    public void processFile(String filepath) {
        Path outPath = Paths.get(System.getProperty("user.dir"), "data", "processed_files",
                Paths.get(filepath).getFileName().toString().split("\\.")[0] + ".processed3");
        if (outPath.toFile().exists()) {
            log.info("A processed file for " + filepath + " already exists, aborting...");
            return;
        }

        log.info("Starting processing file " + filepath);

        Timer.setTimer();
        log.info("Cleaning the text in memory...");

        String cleaned = Cleaner.cleanFileInMemory(filepath);
        if (cleaned == null) return;

        log.info("Completed cleaning text...");
        log.info("Beginning analysis...");
        // Use a buffer to avoid using too much mem. use 10mb
        WriteBuffer buffer = new WriteBuffer(outPath, 10000);

        // Reading file line by line.
        Scanner sc = new Scanner(cleaned);

        long[] analysisCounts = analyzeAndWrite(sc, buffer);

        Timer.endTimer();
        log.info("Finished processing file " + filepath);
        log.info("Processed {} sentences and {} tokens in " + Timer.results(), analysisCounts[0], analysisCounts[1]);
    }


    // ****************************** USED FOR TESTING ****************************** \\
    public static void main ( String[] args) throws IOException {
//        System.out.println(pp.analyzeSentence("Nevşehir'deki mitingin ardından Adıyaman'a geçerek yurttaşlarla bir araya geldi."));
        PreProcessor pp = new PreProcessor();
        pp.processFile("data\\corpora\\medium_corpus.txt");
    }
}
