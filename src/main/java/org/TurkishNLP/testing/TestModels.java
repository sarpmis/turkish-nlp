package org.TurkishNLP.testing;

import org.TurkishNLP.word2vec.Word2VecParams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 * This file contains Word2VecParams objects for each test case
 */
public class TestModels {
    private Word2VecParams test1, test2, test3, test4, test5, test6;

    public Collection<Word2VecParams> tests = new ArrayList();

    public TestModels() {
        test1 = new Word2VecParams("n_10epoch_250layer_10min_15neg")
            .setNumEpochs(10)
            .setLayerSize(250)
            .setMinWordFrequency(10)
            .setNegativeSampling(15);
        tests.add(test1);

//        test2 = new Word2VecParams("n_10epoch_250layer_10min_10neg")
//            .setNumEpochs(10)
//            .setLayerSize(250)
//            .setMinWordFrequency(10)
//            .setNegativeSampling(10);
//        tests.add(test2);
//
//        test3 = new Word2VecParams("n_5epoch_200layer_10min_10neg")
//            .setNumEpochs(5)
//            .setLayerSize(200)
//            .setMinWordFrequency(10)
//            .setNegativeSampling(10);
//        tests.add(test3);
//
//        test4 = new Word2VecParams("n_5epoch_300layer_10min_10neg")
//            .setNumEpochs(5)
//            .setLayerSize(300)
//            .setMinWordFrequency(10)
//            .setNegativeSampling(10);
//        tests.add(test4);
//
//        test5 = new Word2VecParams("n_5epoch_250layer_10min_15neg")
//            .setNumEpochs(5)
//            .setLayerSize(250)
//            .setMinWordFrequency(10)
//            .setNegativeSampling(15);
//        tests.add(test5);
//
//        test6 = new Word2VecParams("n_5epoch_250layer_5min_15neg")
//            .setNumEpochs(5)
//            .setLayerSize(250)
//            .setMinWordFrequency(5)
//            .setNegativeSampling(15);
//        tests.add(test6);
    }

    // sets the corpus path for all tests
    public void setCorpus(String corpusPath) {
        tests.forEach(p -> p.setCorpusPath(corpusPath));
    }

    // sets the dictionary path for all tests
    public void setDictionary(String dictionaryPath) {
        tests.forEach(p -> p.setDictionaryPath(dictionaryPath));
    }

    public List<String> getModelNames() {
        List<String> l = new ArrayList<>();
        tests.forEach(t -> l.add(t.getName()));
        return l;
    }
}
