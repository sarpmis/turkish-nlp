package org.TurkishNLP;

import org.TurkishNLP.testing.Test;
import org.TurkishNLP.testing.TestModels;
import org.TurkishNLP.testing.Tester;
import org.TurkishNLP.testing.impl.AnalogyTest;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.TurkishNLP.word2vec.Word2VecParams;
import org.TurkishNLP.word2vec.model_utils.BetterModelUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ModelTrainerMain {
    public static void main(String[] args) throws IOException {
        // set up testing tools
        Tester t = new Tester();
        List<Test> tests;
        TestModels models = new TestModels();
        models.setCorpus("data\\processed_files\\normalized.lemma");

        for(Word2VecParams p : models.tests) {
            Word2VecModel m;
            try {
                m = Word2VecModel.readModelByName(p.getName());
            } catch (FileNotFoundException e) {
                m = Word2VecModel.initializeWithParams(p);
                Word2VecModel.trainModel(m.getWord2Vec(), p.getCorpusPath());
            }

            m.getWord2Vec().setModelUtils(new BetterModelUtils());

            tests = AnalogyTest.readAnalogyTests("data\\testing\\analogy_tests\\antonyms.txt");
            t.runTestsOnModel(m, tests, new PrintWriter(new File("data\\testing\\out\\" + m.getName() + "_antonyms.txt")));

            // run capital tests
            tests = AnalogyTest.readAnalogyTests("data\\testing\\analogy_tests\\capitals.txt");
            t.runTestsOnModel(m, tests, new PrintWriter(new File("data\\testing\\out\\" + m.getName() + "_capitals.txt")));

            // run gender tests
            tests = AnalogyTest.readAnalogyTests("data\\testing\\analogy_tests\\gender.txt");
            t.runTestsOnModel(m, tests, new PrintWriter(new File("data\\testing\\out\\" + m.getName() + "_gender.txt")));

            // save the model
            Word2VecModel.saveModel(m, true);
        }
    }
}
