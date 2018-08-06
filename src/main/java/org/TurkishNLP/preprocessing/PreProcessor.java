package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.core.turkish.PrimaryPos;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PreProcessor {

    private TurkishMorphology morphology;
    private TurkishSentenceExtractor extractor;

    public PreProcessor() throws IOException {
        morphology = TurkishMorphology.createWithDefaults();
        extractor = TurkishSentenceExtractor.DEFAULT;
    }

    public String removeUnwantedCharacters(String str){
        // only alphanumeric characters, whitespace or sentence-ending punctuation
        str = str.replaceAll("[^\\p{L}\\s\\p{N}.,?!']", " ");
        // remove single letter/number words that may be floating around
        str = str.replaceAll("(\\s[\\pL\\pN][\\s\\.\\?!,])", " ");
        str = removeExtraSpaces(str);
        return str;
    }

    public String removeExtraSpaces(String str) {
        return str
                .trim()
                .replaceAll(" +", " ")
                .replaceAll("\n+", "\n")
                .replaceAll("(\n )", "\n");
        // TODO: remove extra spaces near punctuation
    }

    public List<DictionaryItem> analyzeSentence(String s) {
        List<DictionaryItem> lst = new ArrayList<>();

        List<WordAnalysis> analyses = morphology.analyzeSentence(s);

        // Can't include words that zemberek can't find in the dictionary
        // or else disambiguate breaks
        // THIS SEEMS TO BE FIXED IN 0.15
        // analyses.removeIf(a -> a.analysisCount() == 0);

//        try {
        SentenceAnalysis result = morphology.disambiguate(s, analyses);

        // Build a list from the output of disambiguate.
        for (SingleAnalysis sa : result.bestAnalysis()) {
            lst.add(sa.getDictionaryItem());
        }
//
//        } catch(IllegalArgumentException e){
//            log.info("No dictionary items found for sentence = " + s);
//        }
        return lst;
    }

    public void processFile(String filepath) throws IOException {
        log.info("Starting processing file " + filepath);
        long lineCount = 0, tokenCount = 0;
        Timer.setTimer();

        Path outPath = Paths.get(System.getProperty("user.dir"), "data", "processed_files",
                Paths.get(filepath).getFileName().toString().split("\\.")[0] + ".processed2");
        if (outPath.toFile().exists()) {
            log.info("A processed file for " + filepath + " already exists, aborting");
            return;
        }

        // Use a buffer to avoid using too much mem. use 10mb
        WriteBuffer buffer = new WriteBuffer(outPath, 10000);

        // Reading file line by line.
        Scanner sc = new Scanner(new File(filepath),"UTF-8");
        String currentLine;
        while(sc.hasNextLine()){
            currentLine = sc.nextLine();

            // Extract sentences from current line.
            List<String> sentences = extractor.fromParagraph(currentLine);

            for(String s : sentences){
                buffer.add(s + " #");
                for(DictionaryItem item : analyzeSentence(s)){
                    if(item.primaryPos.equals(PrimaryPos.Punctuation)) {
                        buffer.add(" " + item.id);
                        tokenCount++;
                    }
                }
                buffer.add(System.lineSeparator());
                lineCount++;
            }
        }
        buffer.finish();
        Timer.endTimer();
        log.info("Finished processing file " + filepath);
        log.info("Processed " + lineCount + " lines and " + tokenCount + " tokens in " + Timer.results());
    }


    // ****************************** USED FOR TESTING ****************************** \\
    public static void main ( String[] args) throws IOException {
//        System.out.println(pp.analyzeSentence("Nevşehir'deki mitingin ardından Adıyaman'a geçerek yurttaşlarla bir araya geldi."));
        PreProcessor pp = new PreProcessor();
    }
}
