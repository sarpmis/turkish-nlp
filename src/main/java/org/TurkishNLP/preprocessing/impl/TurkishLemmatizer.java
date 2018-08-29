package org.TurkishNLP.preprocessing.impl;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.preprocessing.ParallelPreProcessor;
import org.TurkishNLP.preprocessing.ParallelizablePreProcessor;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.tokenization.TurkishSentenceExtractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/*
 * Class for lemmatizing Turkish text using Zemberek library and its built in Turkish dictionary.
 * Assumes that sentences do not wrap on to next lines.
 * Words that cannot be found in Zemberek dictionary will be replaced with UNK token
 */
@Slf4j
public class TurkishLemmatizer extends ParallelizablePreProcessor {
    private static final int LOG_PROGRESS_FREQ = 1000000;

    private TurkishMorphology morphology;
    private TurkishSentenceExtractor extractor;

    // Zemberek has a very chatty logger so we disable it. It can be enabled again later if necessary
    public static final Logger CHATTY = Logger.getLogger("zemberek-logger");

    public TurkishLemmatizer() {
        if(CHATTY.getLevel() == null || !CHATTY.getLevel().equals(Level.OFF)) {
            CHATTY.setLevel(Level.OFF);
            log.info("Disabled Zemberek logging");
        }
        morphology = TurkishMorphology.createWithDefaults();
        extractor = TurkishSentenceExtractor.DEFAULT;
    }

    private List<DictionaryItem> analyzeSentence(String s) {
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

    private List<DictionaryItem> analyzeLine(String line) {
        List<DictionaryItem> ret = new ArrayList<>();
        List<String> sentences = extractor.fromParagraph(line);
        for(String s : sentences) {
            for(DictionaryItem item : analyzeSentence(s)){
                ret.add(item);
            }
        }
        return ret;
    }

    /*
     * writes a the list of dictionary items and makes a newline
     */
    private void writeAnalyzedLine(List<DictionaryItem> items, PrintWriter out) {
        if(items.size() > 0) out.print(items.get(0).getId());
        for(int i = 1; i < items.size(); i++) {
            out.print(" " + items.get(i).getId());
        }
        out.println();
    }

    private List<DictionaryItem> filterNumbers(List<DictionaryItem> items) {
        return items.stream()
                .filter(i -> !i.primaryPos.equals(PrimaryPos.Numeral))
                .collect(Collectors.toList());
    }

    private List<DictionaryItem> filterPunctuation(List<DictionaryItem> items) {
        return items.stream()
                .filter(i -> !i.primaryPos.equals(PrimaryPos.Punctuation))
                .collect(Collectors.toList());
    }

    private List<DictionaryItem> filterUnknown(List<DictionaryItem> items) {
        return items.stream()
                .filter(i -> !i.primaryPos.equals(DictionaryItem.UNKNOWN.primaryPos))
                .collect(Collectors.toList());
    }

    @Override
    public boolean processFile(File input, File output) {
        try(
            Scanner in = new Scanner(input);
            PrintWriter out = new PrintWriter(output)
        ) {
            long analysisCount = 0;
            String line;
            while(in.hasNextLine()) {
                line = in.nextLine();
                List<DictionaryItem> analyzedLineItems = analyzeLine(line);
                analyzedLineItems = filterUnknown(filterNumbers(filterPunctuation(analyzedLineItems)));
                writeAnalyzedLine(analyzedLineItems, out);

                // logic for logging progress
                long nextCount = analysisCount + analyzedLineItems.size();
                if(nextCount % LOG_PROGRESS_FREQ < analysisCount % LOG_PROGRESS_FREQ) {
                    log.info("Analyzed item count = {}", nextCount);
                }
                analysisCount = nextCount;
            }
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public String processLine(String input) {
        List<DictionaryItem> items = analyzeLine(input);
        items = filterUnknown(filterNumbers(filterPunctuation(items)));
        StringBuilder b = new StringBuilder();
        if(items.size() > 0) b.append(items.get(0).normalizedLemma());
        for(int i = 1; i < items.size(); i++) {
            b.append(" " + items.get(i).normalizedLemma());
        }
        return b.toString();
    }

    public static void main(String[] args) {
        ParallelPreProcessor<TurkishLemmatizer> pp = new ParallelPreProcessor<>(TurkishLemmatizer.class);
        pp.processFile("data\\processed_files\\gensim_parallel.clean", "data\\processed_files\\normalized.lemma");
    }
}
