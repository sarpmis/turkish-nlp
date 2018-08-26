package org.TurkishNLP.test_cases;

import it.unimi.dsi.fastutil.objects.AbstractReferenceList;
import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.TurkishNLP.word2vec.Word2VecParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
                System.getProperty("user.dir"), "data", "dictionaries", "remaining.dict").toString());
        cases.setCorpus(Paths.get(
                System.getProperty("user.dir"), "data", "processed_files", "trwiki_corpus.processed").toString());
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
            Word2VecModel.trainModel(model.getWord2Vec(), testParams.getCorpusPath());
            log.info("Model: {} training complete. Starting saving model...", model);
            Word2VecModel.saveModel(model, false);
            log.info("Saving completed");
        }
    }

    public void runTestsOnModel(Word2VecModel model, Collection<Test> tests, PrintWriter out) {
        List<TestResults> results = new ArrayList<>();
        tests.forEach(t -> results.add(t.run(model)));
        out.println("**** Tests for model: " + model + " ****" + System.lineSeparator());
        List<Double> scores = new ArrayList<>();
        results.forEach(r -> {
            out.println(r.getMessage());
            if(!r.getScore().equals(Double.NaN)) {
                scores.add(r.getScore());
            }
        });
        if(scores.size() > 0) {
            Statistics stats = new Statistics(scores);
            out.println("****** SUMMARY ******");
            out.println(stats.getSize() +  " scores analyzed.");
            out.println("Min= [" + stats.getMin() + "]");
            out.println("Max= [" + stats.getMax() + "]");
            out.println("Mean= [" + stats.getMean() + "]");
            out.println("Standard Deviation= [" + stats.getStandardDeviation() + "]");
        }
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
            try {
                m = Word2VecModel.readModelByName(modelName);
                runTestsOnModel(m, tests, out);
            } catch (FileNotFoundException e) {
                log.error("Model [{}] not found, skipping");
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
}
