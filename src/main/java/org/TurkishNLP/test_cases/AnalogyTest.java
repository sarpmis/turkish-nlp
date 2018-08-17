package org.TurkishNLP.test_cases;

import org.TurkishNLP.word2vec.Word2VecModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnalogyTest implements Test {
    // A is to B as C is to () -> should return D
    // Ex. Paris is to France as Rome is to Italy
    private String a,b,c,d;
    private int top;
    private String res;

    /*
     * @pre top < model.vocabCount()s
     */
    public AnalogyTest(String a, String b, String c, String d, int top) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.top = top;
    }

    public void run(Word2VecModel model) {
        int vocabCount = model.getVocabCount();
        List<String> results = model.getClosest(Arrays.asList(a, c), Arrays.asList(b), vocabCount)
                .stream()
                .collect(Collectors.toList());

        StringBuilder b = new StringBuilder();
        int index = results.indexOf(d);
        b.append("Test:" + a + " is to " + b + " as " + c + " is to (" + d + ")?" + System.lineSeparator());
        b.append("Expected word " + d + " is the top " + index + " closest word" + System.lineSeparator());
        b.append("  List of " + top + " closest words = ");
        for(int i = 0; i < top; i++) {
            String word = results.get(i);
            b.append(i + ": " + word + " ");
        }
        b.append(System.lineSeparator());
        res = b.toString();
    }

    public String results() {
        return res;
    }
}
