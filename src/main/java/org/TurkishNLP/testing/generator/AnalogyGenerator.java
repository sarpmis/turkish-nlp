package org.TurkishNLP.testing.generator;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.preprocessing.impl.TurkishLemmatizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Class to easily create analogy tests from binary word relations
 */
@Slf4j
public class AnalogyGenerator {
    public List<StringRelation> readRelationsFromFile(String filePath) throws FileNotFoundException {
        return readRelationsFromFile(new File(filePath));
    }

    /**
     * Reads relations from file. Format: on each line the starting point of relation and the ending point
     * in space-separated format. Ex:
     * fransa paris
     * @param file while containing the relations
     * @throws FileNotFoundException
     */
    public List<StringRelation> readRelationsFromFile(File file) throws FileNotFoundException {
        List<StringRelation> relations = new ArrayList<>();
        int count = 0;
        Scanner s = new Scanner(file);
        String line;
        log.info("Reading relations from file [{}]", file);
        while(s.hasNextLine()) {
            line = s.nextLine();
            if(line.startsWith("%")) continue;
            String[] rel = line.split(" ");
            if(rel.length == 2) {
                relations.add(new StringRelation(rel[0], rel[1]));
                count++;
            }
        }
        log.info("Read [{}] relations from file", count);
        return relations;
    }

    /**
     * Generates analogy tests from given list of String relations. Creates a an analogy for each pair of relations.
     * Analogy tests are in the form: (from1 - to1 + to2 = [from2]?)
     * If reverse is set to true all relations are reversed: (to1 - from1 + from2 = [to2]?)
     * @param relations list of relations
     * @param reverse should relations be reversed
     * @return analogy tests ready to be parsed by its parser
     */
    public String generateTests(@NonNull List<StringRelation> relations, boolean reverse) {
        int totalRelations = 0;
        log.info("Genarating analogy tests from [{}] relations", relations.size());
        relations = relations.stream().filter(r -> !(r == null)).collect(Collectors.toList());
        StringBuilder bld = new StringBuilder();
        for(int i = 0; i < relations.size(); i++) {
            StringRelation rel = relations.get(i);
            for(int j = 0; j < relations.size(); j++) {
                if(i == j) continue;

                StringRelation otherRel = relations.get(j);

                if(rel.getFrom().equals(otherRel.getFrom()) || rel.getTo().equals(otherRel.getTo())) continue;

                String a, b, c, d;
                a = rel.getFrom();
                b = rel.getTo();
                c = otherRel.getTo();
                d = otherRel.getFrom();

                if(reverse) {
                    bld.append(b + " " + a + " " + d + " " + c);
                } else {
                    bld.append(a + " " + b + " " + c + " " + d);
                }

                bld.append(System.lineSeparator());
                totalRelations ++;
            }
        }
        log.info("Done. Generated [{}] tests", totalRelations);
        return bld.toString();
    }

    public String lemmatizeTests(String tests) {
        TurkishLemmatizer t = new TurkishLemmatizer();
        StringBuilder b = new StringBuilder();
        String[] lines = tests.split("\n");
        Arrays.stream(lines).forEach(l -> {
            b.append(t.processLine(l));
            b.append(System.lineSeparator());
        });
        return b.toString();
    }

    private class StringRelation {
        private String from;
        private String to;

        public StringRelation(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String toString() {
            return from + " -> " + to;
        }
    }

    public static void main(String[] args) {
        TurkishLemmatizer t = new TurkishLemmatizer();
        AnalogyGenerator g = new AnalogyGenerator();

        // lemmatized?
        boolean lemma = false;
        String source = "capitals";

        try {
            // source
            List<StringRelation> relations = g.readRelationsFromFile("data\\testing\\relations\\relations_" + source + ".txt");
            // destination
            PrintWriter pw = new PrintWriter("data\\testing\\analogy_tests\\" + source +
                    (lemma ? "_lemma.txt" : ".txt"));


            String[] lines = g.generateTests(relations, false).split(System.lineSeparator());
            Arrays.stream(lines).forEach(l -> {
                pw.println((lemma ? t.processLine(l) : l));
            });
            pw.close();
        } catch (FileNotFoundException e) {
            System.out.println("can't find file");
        }
    }
}
