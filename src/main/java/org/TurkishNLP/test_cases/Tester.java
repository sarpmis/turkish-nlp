package org.TurkishNLP.test_cases;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.TurkishNLP.word2vec.Word2VecParams;
import org.TurkishNLP.word2vec.Word2VecTrainer;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

@Slf4j
public class Tester {
    TestModels cases;

    public Tester(){
        cases = new TestModels();
        // dictionary is trimmed with min word freq of 5
        cases.setDictionary(Paths.get(
                System.getProperty("user.dir"), "data", "dictionaries", "remaining.dict"));
        cases.setCorpus(Paths.get(
                System.getProperty("user.dir"), "data", "processed_files", "medium_corpus.processed2"));
    }

    public void readTests(File target) {

    }

    public void trainTestModels() {
        log.info("Starting test model training...");
        for(Word2VecParams testParams : cases.tests) {
            Word2VecModel model = Word2VecModel.initializeWithParams(testParams);
            log.info("Starting training model: {}", model);
            Word2VecTrainer.trainModel(model.getWord2Vec(), testParams.getCorpusPath());
            log.info("Model: {} training complete. Starting saving model...", model);
            Word2VecModel.saveModel(model);
            log.info("Saving completed");
        }
    }

    public void runTestsOnModel(Word2VecModel model, Collection<Test> tests, PrintWriter out) {
        out.println("**** Tests for model: " + model + " ****");
        tests.forEach(t -> {
            t.run(model);
            out.println(t.results());
        });
    }

    // In memory implementation
    public void runTestsOnMultipleModels(Collection<Word2VecModel> models, Collection<Test> tests, PrintWriter out) {
        models.forEach(m -> runTestsOnModel(m, tests, out));
    }

    // On disk implementation
    public void runTestsOnDisk(Collection<String> modelNames, Collection<Test> tests, PrintWriter out) {
        Word2VecModel m;
        for(String modelName : modelNames) {
            m = Word2VecModel.readModel(modelName);
            if(m != null) {
                runTestsOnModel(m, tests, out);
            }
        }
    }

    // trains test models and then runs tests on disk
    public void trainAndRunTest(Collection<Test> tests, PrintWriter out) {
        trainTestModels();
        runTestsOnDisk(cases.getModelNames(), tests, out);
    }

    public static void main(String[] args) {
        Tester testy = new Tester();
        testy.trainTestModels();
    }
}
