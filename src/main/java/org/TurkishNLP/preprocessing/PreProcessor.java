package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.core.turkish.PrimaryPos;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PreProcessor {

    protected TurkishMorphology morphology;
    protected TurkishSentenceExtractor extractor;

    public PreProcessor() throws IOException {
        morphology = TurkishMorphology.createWithDefaults();
        extractor = TurkishSentenceExtractor.DEFAULT;
    }

    public void processFile(String filepath) throws IOException {

        Path outPath = Paths.get(System.getProperty("user.dir"), "data", "processed_files",
                Paths.get(filepath).getFileName().toString().split("\\.")[0] + ".processed");
        if (outPath.toFile().exists()) {
            Files.delete(outPath);
        }

        // Use a buffer to avoid using too much mem. use 10mb
        WriteBuffer buffer = new WriteBuffer(outPath, 10000);

        // Reading file line by line.
        Scanner sc = new Scanner(new File(filepath));
        String currentLine, lastSentence = "";

        while(sc.hasNextLine()){
            currentLine = sc.nextLine();
            // If last sentence on previous line wrapped to this line
            // prepend that (half) sentence to currentLine.
            if (!lastSentence.matches(".*\\p{Punct}")) {
                currentLine = lastSentence + " " + currentLine;
            }
            // Extract sentences from current line.
            List<String> sentences = extractor.fromParagraph(currentLine);
            if(sentences.size()>0) lastSentence = sentences.get(sentences.size()-1);
            else lastSentence = "";

            sentences = cleanSentences(sentences);

            for(String s : sentences){
//                List<WordAnalysis> analyses = morphology.analyzeSentence(s);
//
//                // Can't include words that zemberek can't find in the dictionary
//                // or else disambiguate breaks
//                analyses.removeIf(a -> a.analysisCount() == 0);
//
//                try {
//                    SentenceAnalysis result = morphology.disambiguate(s, analyses);
//
//                    // Build a string from the output of disambiguate.
//                    String temp = "";
//                    for (SingleAnalysis sa : result.bestAnalysis()) {
//                        temp += sa.getDictionaryItem().id + " ";
//                    }
//                    buffer.add(temp + System.lineSeparator());
//                } catch(IllegalArgumentException e){
////                    System.out.println(e.toString() + " Sentence = " + s);
//                }
                analyzeSentence(s).stream().forEach(word -> buffer.add(word));
                buffer.add(System.lineSeparator());
            }
        }
        buffer.finish();
    }

    /*
     * Filter out sentences that don't end in punctuation,
     * remove every non-letter/whitespace
     */
    private static List<String> cleanSentences(List<String> sentences){
        List<String> result = sentences.stream()
                .filter(s -> s.matches(".*\\p{Punct}"))
                .map(s -> s.replaceAll("[^\\p{L}\\s]", ""))
                .collect(Collectors.toList());
        result.removeIf(s -> StringUtils.isBlank(s));
        return result;
    }

    /*
     * Creates a dictionary file that contains the id for each
     * item in the dictionary in a separate line.
     */
    public void processDictionary(String outputPath) throws IOException {
        // Delete the file if it exists
        Path outPath = Paths.get(outputPath);
        if (outPath.toFile().exists()) {
            Files.delete(outPath);
        }
        // Build string
        final StringJoiner joiner = new StringJoiner("\n");
        morphology.getLexicon().iterator().forEachRemaining(item -> {
            // Filter punctuation
            if (!item.primaryPos.equals(PrimaryPos.Punctuation)) joiner.add(item.id);
        });
        // Write to file
        Files.write(outPath, joiner.toString().getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        System.out.println("ProcessedDictionary");
    }

    public String processString(String s){
        return null;
    }

    public List<String> analyzeSentence(String s){
        List<String> lst = new ArrayList<>();

        // Only letters and whitespaces
        s = s.replaceAll("[^\\p{L}\\s]", "");

        List<WordAnalysis> analyses = morphology.analyzeSentence(s);

        // Can't include words that zemberek can't find in the dictionary
        // or else disambiguate breaks
        analyses.removeIf(a -> a.analysisCount() == 0);

        try {
            SentenceAnalysis result = morphology.disambiguate(s, analyses);

            // Build a list from the output of disambiguate.
            for (SingleAnalysis sa : result.bestAnalysis()) {
                lst.add(sa.getDictionaryItem().id);
            }

        } catch(IllegalArgumentException e){
            log.error("Something went wrong while analyzing sentence = " + s);
        }
        return lst;
    }


    // ****************************** USED FOR TESTING ****************************** \\
    public static void main ( String[] args) throws IOException {
         // Zemberek classes used for nlp processing.
        PreProcessor pp = new PreProcessor();
////        PreProcessor.processFile("src\\main\\java\\org\\TurkishNLP\\preprocessing\\sample_texts\\short.txt", morphology, extractor);
//        PreProcessor.processFile("C:\\Dev\\nim_programs\\wiki2text\\trwiki.txt", morphology, extractor);
        System.out.println(pp.analyzeSentence("Nevşehir'deki mitingin ardından Adıyaman'a geçerek yurttaşlarla bir araya geldi."));
    }
}
