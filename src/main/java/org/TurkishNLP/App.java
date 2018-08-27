package org.TurkishNLP;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.preprocessing.ParallelPreProcessor;
import org.TurkishNLP.preprocessing.impl.TurkishLemmatizer;
import org.TurkishNLP.shared.Timer;
import org.TurkishNLP.test_cases.Test;
import org.TurkishNLP.test_cases.Tester;
import org.TurkishNLP.test_cases.impl.AnalogyTest;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.TurkishNLP.word2vec.Word2VecParams;
import org.TurkishNLP.word2vec.model_utils.BasicModelUtils2;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 *
 */
@Slf4j
public class App {
    public static void main( String[] args ) throws IOException {
        String lemmatized = "5epoch_250layer_10min_10neg";
        String non_lemmatized = "gensim3";
        String lemmatizedTest = "data\\testing\\lemma_tests.txt";
        String non_lemmatizedTest = "data\\testing\\non_lemma_tests.txt";

        /**
         * PREPROCESS
         */
//        ParallelPreProcessor<TurkishLemmatizer> pp = new ParallelPreProcessor<>(TurkishLemmatizer.class);
//        pp.processFile("data\\processed_files\\gensim_parallel.clean", "data\\processed_files\\gensim_parallel_noUNK.lemma");

        /**
         * PARAM
         */
        Word2VecParams p = new Word2VecParams(lemmatized)
                .setNumEpochs(5)
                .setNegativeSampling(5)
                .setMinWordFrequency(5)
                .setWindowSize(5)
                .setLayerSize(400)
                .setSubSampling(0.001)
                .setCorpusPath("data" + File.separator + "corpora" +
                        File.separator + "gensim_no_punc.txt");
//                .setCorpusPath("data" + File.separator +   "processed_files" +
//                        File.separator + "gensim_parallel_noUNK.lemma");

        /**
         * LOAD
         */
        Word2VecModel m = Word2VecModel.readModelByName(p.getName());

        /**
         * TESTING NEW MODEL UTILS
         */
//        BasicModelUtils2 utils = new BasicModelUtils2();
//        utils.init(m.getWord2Vec().lookupTable());

//        utils.wordsNearestScored(Arrays.asList("futbol_Noun"), new ArrayList<>(), 10);

//        System.out.println(m.getWord2Vec().wordsNearestSum("futbol_Noun", 10));
//        System.out.println(utils.wordsNearestSum("futbol_Noun", 10));

        /**
         * Train
         */
//        Word2VecModel m = Word2VecModel.initializeWithParams(p);
//        Timer.TimerToken tt = Timer.newToken();
//        log.info("starting training");
//        Word2VecModel.trainModel(m.getWord2Vec(), p.getCorpusPath());
//        log.info("training finished in " + Timer.checkOut(tt));
//        tt = Timer.newToken();
//        log.info("saving model");
//        Word2VecModel.saveModel(m, true);
//        log.info("saving finished in " + Timer.checkOut(tt));

        /**
         * RUN TESTS
         */
        Tester t = new Tester();
//        List<Test> tests = Tester.readTests(non_lemmatizedTest);
        List<Test> tests = AnalogyTest.readAnalogyTests("data\\testing\\analogy_tests\\capitals_lemma.txt");
        t.runTestsOnModel(m, tests, new PrintWriter(new File("data\\testing\\out\\" + m.getName() + "_capitals.txt")));
    }
}
