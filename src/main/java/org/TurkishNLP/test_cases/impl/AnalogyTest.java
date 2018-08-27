package org.TurkishNLP.test_cases.impl;

import org.TurkishNLP.test_cases.Test;
import org.TurkishNLP.test_cases.TestResults;
import org.TurkishNLP.word2vec.Word2VecModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class AnalogyTest implements Test {
    // A is to B as C is to () -> should return D
    // Ex. Paris is to France as Rome is to Italy
    private String a,b,c,d;
    private final int TOP_WORDS_TO_SHOW = 10;
    private String res;

    /**
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
        Collection<String> closest = model.getClosest(Arrays.asList(a, c), Arrays.asList(b), vocabCount);
        if(closest == null) {
            res.setMessage("One or more input words not in vocab");
            return res;
        }
        // collect it to string
        List<String> resList = closest.stream().collect(Collectors.toList());
        StringBuilder builder = new StringBuilder();
        // filter the input words
        resList.removeAll(Arrays.asList(a,b,c));
        int index = resList.indexOf(d) + 1;
        if(index == 0) {
            res.setMessage("[ +" + d + "] is not in vocab");
            return res;
        }
        builder.append("AnalogyTest: [" + a + "] - [" + b + "] + [" + c + "] = [" + d + "]?" + System.lineSeparator());
        builder.append("  " + d + " is the top *****[" + index + "]*****" + System.lineSeparator());
        builder.append("  List of " + TOP_WORDS_TO_SHOW + " closest words = ");
        for(int i = 0; i < TOP_WORDS_TO_SHOW; i++) {
            String word = resList.get(i);
            builder.append("[" + (i+1) + ": " + word + "] ");
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
