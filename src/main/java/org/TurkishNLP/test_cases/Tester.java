package org.TurkishNLP.test_cases;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.TurkishNLP.word2vec.Word2VecParams;
import org.TurkishNLP.word2vec.Word2VecTrainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

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

    // Reads tests from file
    public static List<Test> readTests(File target) throws FileNotFoundException {
        List<Test> tests = new ArrayList<>();
        Scanner sc = new Scanner(target);
        String line;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            if(line.startsWith("%") || line.isEmpty()) continue;
            Test t = Test.parseTest(line);
            if(t == null) log.error("Can't parse test from line " + line);
            else tests.add(t);
        }
        return tests;
    }

    public static List<Test> readTests(String filePath) throws FileNotFoundException {
        return readTests(Paths.get(filePath).toFile());
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
        out.println("**** Tests for model: " + model + " ****" + System.lineSeparator());
        tests.forEach(t -> {
            t.run(model);
            out.println(t.results() + System.lineSeparator());
        });
        out.flush();
    }

    // In memory implementation
    public void runTestsOnModels(Collection<Word2VecModel> models, Collection<Test> tests, PrintWriter out) {
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
    public void readAndRunTests(String testFilePath, String outPath) {
        try {
            PrintWriter out = new PrintWriter(outPath);
            List<Test> tests = this.readTests(Paths.get(testFilePath).toFile());
            runTestsOnDisk(cases.getModelNames(), tests, out);
        } catch(FileNotFoundException e) {
            log.error("Test file not found at: " + testFilePath);
        }
    }

    public static void main(String[] args) throws IOException {
        Tester testy = new Tester();
        testy.trainTestModels();
//        PrintWriter printy = new PrintWriter("data\\testing\\out.txt");
//        testy.runTestsOnModel(Word2VecModel.readModel("trimmed_dictionary"),
//                Tester.readTests("data\\testing\\basic_tests.txt"), printy);
//        testy.readAndRunTests("data\\testing\\basic_tests.txt", "data\\testing\\out.txt");
    }
}
