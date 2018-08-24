package org.TurkishNLP;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.preprocessing.ParallelPreProcessor;
import org.TurkishNLP.preprocessing.impl.TextCleaner;
import org.TurkishNLP.preprocessing.impl.TurkishLemmatizer;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.TurkishNLP.word2vec.Word2VecParams;
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
//        boolean success = false;
//        ParallelPreProcessor<TextCleaner> cleaner = new ParallelPreProcessor<>(TextCleaner.class);
//        success = cleaner.processFile("data\\corpora\\gensim_no_clean.txt", "data\\processed_files\\gensim_parallel.clean");
//
//        if(success) {
//            ParallelPreProcessor<TurkishLemmatizer> lemmatizer = new ParallelPreProcessor<>(TurkishLemmatizer.class);
//            success = lemmatizer.processFile("data\\processed_files\\gensim_parallel.clean", "data\\processed_files\\gensim_parallel.lemma");
//        }

        Word2VecParams p = new Word2VecParams("gensim_parallel")
                .setNumEpochs(5)
                .setNegativeSampling(5)
                .setMinWordFrequency(5)
                .setWindowSize(5)
                .setLayerSize(400)
                .setSubSampling(0.001)
                .setCorpusPath("data\\processed_files\\gensim_parallel.lemma");

    }
}
