package org.TurkishNLP.dict;

import lombok.extern.slf4j.Slf4j;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.DictionaryItem;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Dictionary {
    public static final String DEFAULT_DICT_NAME = "master";
    public static final String DICTIONARY_EXTENSION = ".dict";
    public static final Path DEFAULT_DICT_PATH = Paths.get("data\\dictionaries\\" + DEFAULT_DICT_NAME + DICTIONARY_EXTENSION);

    // Process zemberek's default dictionary. ONLY NEEDS TO BE DONE ONCE
    public static void processDefaultDictionary() {
        try {
            PrintWriter pw = new PrintWriter(DEFAULT_DICT_PATH.toFile());
            TurkishMorphology.createWithDefaults()
                    .getLexicon()
                    .iterator()
                    .forEachRemaining(
                        item -> pw.println(item.getId())
                    );
            pw.print(DictionaryItem.UNKNOWN.getId());
            pw.close();
        } catch(IOException e) {
            log.error("An error occurred while processing dictionary...");
        }
    }

    public static HashMap<String, Long> getDefaultFrequencyHash(){
        try {
            HashMap<String, Long> map = new HashMap<>();
            Scanner s = new Scanner(DEFAULT_DICT_PATH.toFile());
            while(s.hasNextLine()){
                map.put(s.nextLine(), new Long(0));
            }
            return map;
        } catch(IOException e) {
            log.error("An error occurred reading default dictionary, may not have been initialized...");
            return null;
        }
    }

    // Starting from the default dictionary, trims items that occur less than maxFreq
    public static HashMap<String, Long> trimDefaultDictionaryWithCorpus(int maxFreq, File corpusFile) throws IOException {
        try {
            log.info("Starting trimming corpus " + corpusFile);
            Scanner s = new Scanner(corpusFile);
            HashMap<String, Long> freqs = getDefaultFrequencyHash();
            log.info("Initial dictionary size = {}", freqs.size());
            String line;
            String processedLine;
            long wordCount = 0, sentenceCount = 0;
            while(s.hasNextLine()) {
                line = s.nextLine();
                sentenceCount++;
//                processedLine = line.split("#")[1];
                processedLine = line;
                for(String processedWord : Arrays.stream(processedLine.split(" "))
                        .filter(str -> !str.equals(""))
                        .collect(Collectors.toList())) {
                    freqs.merge(processedWord, (long) 1, Long::sum);
                    wordCount++;
                }
            }
            log.info("Finished reading {} lines and {} words from corpus", sentenceCount, wordCount);
            log.info("Removing low frequency words...");
            log.info("Initial dictionary size = {}", freqs.size());
            int removedCount = 0;
            List<String> toRemove = new ArrayList();
            for(HashMap.Entry<String, Long> k : freqs.entrySet()) {
                if(k.getValue() < maxFreq) {
                    toRemove.add(k.getKey());
                    removedCount++;
                }
            }
            freqs.keySet().removeAll(toRemove);
            log.info("Done removing low freq words. {} words removed. New dictionary size = {}", removedCount, freqs.size());
            printHashDict(freqs, Paths.get("data\\dictionaries\\remaining.dict"));
            printCollectionDict(toRemove, Paths.get("data\\dictionaries\\removed.dict"));
            return freqs;
        } catch (IOException e) {
            log.error("Corpus file does not exist");
            return null;
        }
    }

    private static void printHashDict(HashMap<String,Long> dict, Path path) throws IOException {
        PrintWriter pw = new PrintWriter(path.toFile());
        for(Map.Entry<String, Long> item : dict.entrySet()) {
//            pw.println(item.getKey() + " - " + item.getValue());
            pw.println(item.getKey());
        }
    }

    private static void printCollectionDict(Collection<String> dict, Path path) throws IOException {
        PrintWriter pw = new PrintWriter(path.toFile());
        dict.stream().forEach(item -> pw.println(item));
    }

    public static void main(String[] args) throws IOException{
//        Dictionary.trimDefaultDictionaryWithCorpus(5, Paths.get("data\\processed_files\\trwiki_corpus.processed").toFile());
    }
}
