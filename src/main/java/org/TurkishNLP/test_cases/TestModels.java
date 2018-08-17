package org.TurkishNLP.test_cases;

import org.TurkishNLP.word2vec.Word2VecParams;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 * This file contains Word2VecParams objects for each test case
 */
class TestModels {
    protected Word2VecParams test1;
    protected Word2VecParams test2;
    protected Word2VecParams test3;
    protected Word2VecParams test4;
    protected Word2VecParams test5;

    protected Collection<Word2VecParams> tests = new ArrayList();

    protected TestModels() {
        test1 = new Word2VecParams("epochs_2")
            .setNumEpochs(2);
        tests.add(test1);

        test2 = new Word2VecParams("epochs_5")
            .setNumEpochs(5);
        tests.add(test2);

        test3 = new Word2VecParams("layers_200")
            .setLayerSize(200);
        tests.add(test3);

        test4 = new Word2VecParams("layers_300")
            .setLayerSize(300);
        tests.add(test4);

        test5 = new Word2VecParams("sampling_1e-5")
            .setSubSampling(1e-5);
        tests.add(test5);
    }

    // sets the corpus path for all tests
    protected void setCorpus(Path corpusPath) {
        tests.forEach(p -> p.setCorpusPath(corpusPath));
    }

    // sets the dictionary path for all tests
    protected void setDictionary(Path dictionaryPath) {
        tests.forEach(p -> p.setDictionaryPath(dictionaryPath));
    }

    protected List<String> getModelNames() {
        List<String> l = new ArrayList<>();
        tests.forEach(t -> l.add(t.getName()));
        return l;
    }
}