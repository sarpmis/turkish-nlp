package org.TurkishNLP.testing.impl;

import org.TurkishNLP.shared.MathOps;
import org.TurkishNLP.testing.Test;
import org.TurkishNLP.testing.TestResults;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.TurkishNLP.word2vec.model_utils.BetterModelUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to test Word2Vec models for analogous relations of the form:
 *
 * A is to B as C is to () -> should return D
 * Ex. Paris is to France as Rome is to Italy
 */
public class AnalogyTest implements Test {
    private final int DECIMALS_TO_SHOW = 3;
    private final int TOP_WORDS_TO_SHOW = 5;
    private String a,b,c,d;

    /**
     * @param a positive word
     * @param b negative word
     * @param c positive word
     * @param d target word
     * @pre top < model.vocabCount()s
     */
    public AnalogyTest(String a, String b, String c, String d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public TestResults run(Word2VecModel model) {
        TestResults res = new TestResults();
        int vocabCount = model.getVocabCount();
        // if getting closest failed one of the words is not in vocab
        List<BetterModelUtils.ScoredLabel> resList = model.getClosest(Arrays.asList(a, c), Arrays.asList(b), vocabCount);
        if(resList == null) {
            res.setMessage("One or more input words not in vocab from: " +
                    "[" + a +"] [" + b + "] [" + c + "] [" + d + "]. Check logs");
            return res;
        }
        StringBuilder builder = new StringBuilder();
        int index = -1;
        for(int i = 0; i < resList.size(); i++) {
            if(resList.get(i).getLabel().equals(d)) index = i + 1;
        }
        if(index == -1) {
            res.setMessage("[" + d + "] is not in vocab");
            return res;
        }
        builder.append("[" + a + "] - [" + b + "] + [" + c + "] = [" + d + "]? ----------> [" + index + "] <---------- ");
        for(int i = 0; i < TOP_WORDS_TO_SHOW; i++) {
            BetterModelUtils.ScoredLabel label = resList.get(i);
            builder.append("[" + (i+1) + ": " + label.getLabel() + " (" +
                    MathOps.roundDoubleTo(label.getScore(), DECIMALS_TO_SHOW) + ")] ");
        }
        builder.append(System.lineSeparator());
        res.setScore((double) index);
        res.setMessage(builder.toString());
        return res;
    }

    public static List<Test> readAnalogyTests(String filePath) throws FileNotFoundException {
        return readAnalogyTests(new File(filePath));
    }

    public static List<Test> readAnalogyTests(File file) throws FileNotFoundException {
        List<Test> tests = new ArrayList<>();
        Scanner sc = new Scanner(file);
        String line;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            String[] args = line.trim().split(" ");
            if(args.length != 4) continue;
            tests.add(new AnalogyTest(args[0], args[1], args[2], args[3]));
        }
        return tests;
    }
}
