package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Cleaner {
    public static String removeUnwantedCharacters(String str){
        // only alphanumeric characters, whitespace or sentence-ending punctuation
        str = str.replaceAll("[^\\p{L}\\s\\p{N}.,?!']", " ");
        // remove single letter/number words that may be floating around
        str = str.replaceAll("(\\s[\\pL\\pN][\\s\\.\\?!,])", " ");
        str = removeExtraSpaces(str);
        return str;
    }

    public static String removeExtraSpaces(String str) {
        return str
                .trim()
                .replaceAll(" +", " ")
                .replaceAll("\n+", "\n")
                .replaceAll("(\n )", "\n");
        // TODO: remove extra spaces near punctuation
    }

    public static String cleanString(String str) {
        return removeExtraSpaces(removeUnwantedCharacters(str));
    }

    public static String cleanFileInMemory(Path filePath) {
        try {
            String fileContent = new String(Files.readAllBytes(filePath), "UTF-8");
            String clean = cleanString(fileContent);
            return clean;
        } catch (IOException e) {
            log.error("Bad filepath {}", filePath);
            return null;
        }
    }

    public static String cleanFileInMemory(String filePath) {
        return cleanFileInMemory(Paths.get(filePath));
    }

    public static void cleanFileOnDisk(Path filePath) {
        // TODO
    }

    public static void main(String[] args) {
        Cleaner.cleanFileInMemory("data\\corpora\\medium_corpus.txt");
    }
}
