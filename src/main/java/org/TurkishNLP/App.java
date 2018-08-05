package org.TurkishNLP;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.word2vec.Word2VecInitializer;
import org.TurkishNLP.word2vec.Word2VecOperations;
import org.TurkishNLP.word2vec.Word2VecTrainer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Hello world!
 *
 */
@Slf4j
public class App {
    public static void main( String[] args ) throws IOException {
//        String dictionaryPath = Paths.get(System.getProperty("user.dir"),
//                "data", "processed_files", "dictionary.processed").toString();
//        Word2Vec w = Word2VecInitializer.initializeModel(dictionaryPath);
//
//        log.info("Initialized model with dictionary with " + dictionaryPath);
//
//        Collection<String> lst = w.wordsNearest("tarih_Noun", 10);
//        log.info("Closest words to 'tarih' before training: " + lst);
//
//        String corpusPath = Paths.get(System.getProperty("user.dir"), "data",
//                "processed_files","corpus.processed").toString();
//        Word2VecTrainer.trainModel(w, corpusPath);
//
//        lst = w.wordsNearest("tarih_Noun", 10);
//        log.info("Closest words to 'tarih' after training: " + lst);
//        Word2VecOperations.saveModel(w, "test");

        Word2Vec w = Word2VecOperations.readModel("test");
        List<String> words = Arrays.asList(new String[] {
                "tarih_Noun",
                "futbol_Noun",
                "bilgi_Noun",
                "kil_Noun",
                "çorba_Noun",
                "belgesel_Noun",
                "imrenmek_Verb",
                "Erdoğan_Noun_Prop",
                "Recep_Noun_Prop",
                "Tayyip_Noun_Prop"
        });
        for(String word : words) {
            Collection<String> lst = w.wordsNearest(word, 10);
            lst.forEach(x -> {
                log.info("Similarity between " + word + " and " + x + " = " + w.similarity(word, x));
            });
        }
        log.info(w.similarity("çorba_Noun", "yemek_Noun") + " ");
        log.info(w.similarity("çorba_Noun", "torba_Noun") + " ");
        log.info(w.similarity("çorba_Noun", "yemek_Verb") + " ");
    }
}
