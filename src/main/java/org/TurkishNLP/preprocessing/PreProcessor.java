package org.TurkishNLP.preprocessing;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.core.turkish.PrimaryPos;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class PreProcessor {

    public static void processFile(String filepath, TurkishMorphology morphology, TurkishSentenceExtractor extractor) throws IOException {

        // Create an output file to put the processed text in.
        // TODO: Currently generates output path in here. Make it a parameter
        Path outPath = Paths.get(filepath.split("\\.")[0] + ".processed");
        if (outPath.toFile().exists()) {
            Files.delete(outPath);
        }

        // Reading file line by line.
        Scanner sc = new Scanner(new File(filepath));
        String currentLine, lastSentence = "";
        while(sc.hasNextLine()){
            currentLine = sc.nextLine();
            // If last sentence on previous line did not end (indicated by a
            // punctuation symbol) prepend that (half) sentence to currentLine.
            if (!lastSentence.matches(".*\\p{Punct}")) {
                currentLine = lastSentence + " " + currentLine;
            }
            // Extract sentences from current line.
            List<String> sentences = extractor.fromParagraph(currentLine);
            lastSentence = sentences.get(sentences.size()-1);

            // Each sentence is analyzed, disambiguated and printed
            // on a separate line in the output file.
            for(String s : sentences){
                // We don't want to put a possible half sentence on a line.
                // TODO: Make this better
                if(s.matches(".*\\p{Punct}")){
                    List<WordAnalysis> analyses = morphology.analyzeSentence(s);

                    // Can't include words that zemberek can't find in the dictionary
                    // or disambiguate breaks.
                    // TODO: Find a better way to do this.
                    List<WordAnalysis> noNullAnalyses = new ArrayList<>();
                    for(WordAnalysis a : analyses){
                        if(a.analysisCount() != 0){
                            noNullAnalyses.add(a);
                        }
                    }

                    SentenceAnalysis result = morphology.disambiguate(s, noNullAnalyses);

                    // Build a string from the output of disambiguate.
                    String temp = "";
                    for(SingleAnalysis sa : result.bestAnalysis()){
                        // We don't want punctuation
                        if(!sa.getDictionaryItem().primaryPos.equals(PrimaryPos.Punctuation))
                            temp += sa.getDictionaryItem().id + " ";
                    }
                    // Write line to output file.
                    Files.write(outPath, (temp + System.lineSeparator()).getBytes(),
                            StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                }
            }
        }
        //            TODO: make this lambda exp work instead of for and if statements above
        //            TODO: currently doesn't work because disambiguate throws NullPointerException when no dictionary item is found
        //            sentences.stream()
        //                    .filter(s -> s.matches(".*\\p{Punct}"))
        //                    .forEach(
        //                            s -> morphology.disambiguate(s,morphology.analyzeSentence(s))
        //                                    .bestAnalysis()
        //                                    .stream()
        //                                    .forEach(System.out::println)
        //                    );
    }

    /*
     * Creates a dictionary file that contains the id for each
     * item in the dictionary in a separate line.
     *
     *
     *
     */
    public static void processDictionary(TurkishMorphology morphology, String outputPath) throws IOException {
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


    // ****************************** USED FOR TESTIN ****************************** \\
    public static void main ( String[] args) throws IOException {
//        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
//        System.out.println("Starting test...");
//
//        String sentence = "Kasım ayı 31 gündür.";
//        Log.info("Sentence  = " + sentence);
//        List<WordAnalysis> analyses = morphology.analyzeSentence(sentence);
//
//        Log.info("Sentence word analysis result:");
//        for (WordAnalysis entry : analyses) {
//            Log.info("Word = " + entry.getInput());
//            for (SingleAnalysis analysis : entry) {
//                Log.info(analysis.formatLong());
//            }
//        }
//        SentenceAnalysis result = morphology.disambiguate(sentence, analyses);
//
//        Log.info("\nAfter ambiguity resolution : ");
//        result.bestAnalysis().forEach(Log::info);
//        for(SingleAnalysis s : result.bestAnalysis()){
//            System.out.println(s + " -> Item : " + s.getDictionaryItem().lemma);
//        }

        // FILE STUFF *******************************************

//        Files.createFile(Paths.get("testing.txt"));
//        System.out.println("file should be created");

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
//        PreProcessor.processDictionary(morphology, "dictionary.processed");

        // Zemberek classes used for processing.
        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;

        PreProcessor.processFile("src\\main\\java\\org\\TurkishNLP\\preprocessing\\sample_texts\\short.txt", morphology, extractor);

    }
}
