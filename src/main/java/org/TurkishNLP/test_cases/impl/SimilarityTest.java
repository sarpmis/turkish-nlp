package org.TurkishNLP.test_cases.impl;

import org.TurkishNLP.test_cases.Test;
import org.TurkishNLP.test_cases.TestResults;
import org.TurkishNLP.word2vec.Word2VecModel;

import java.util.HashMap;
import java.util.Map;

/*
 * Simple test that returns the n closest words and their similarities to given word in the model
 */
public class SimilarityTest implements Test {
    String word;
    int n;

    String res;

    public SimilarityTest(String word, int n) {
        this.word = word;
        this.n = n;
    }

    public TestResults run(Word2VecModel m) {
        Map<String, Double> closest = new HashMap<>();
        m.getClosest(word, n)
                .forEach(otherWord -> closest.put(otherWord, m.getSimilarity(word, otherWord)));
        StringBuilder b = new StringBuilder();
        b.append("Closest words to " + word + ": ");
        closest.forEach((otherWord,sim) -> b.append(otherWord + ": " + sim + ", ")
        );
        res = b.toString();
        return new TestResults().setMessage(res);
    }
}
