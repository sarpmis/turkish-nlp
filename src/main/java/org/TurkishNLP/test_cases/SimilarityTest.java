package org.TurkishNLP.test_cases;

import org.TurkishNLP.word2vec.Word2VecModel;

import java.util.Collection;
import java.util.Map;

/*
 * Simple test that returns the n closest words and their similarities to a word in the model
 */
public class SimilarityTest implements Test {
    String word;
    int n;
    Map<String, Double> closest;

    public SimilarityTest(String word, int n) {
        this.word = word;
        this.n = n;
    }

    public void run(Word2VecModel m) {
        m.getClosest(word, n)
                .forEach(otherWord -> closest.put(otherWord, m.getSimilarity(word, otherWord)));
    }

    public String results() {
        StringBuilder b = new StringBuilder();
        closest.forEach((otherWord,sim) -> b.append(otherWord + " " + sim + " ")
        );
        return b.toString();
    }
}
