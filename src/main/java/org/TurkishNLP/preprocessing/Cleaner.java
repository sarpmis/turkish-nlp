package org.TurkishNLP.preprocessing;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

@Slf4j
public class Cleaner {
    private static String removeUnwantedCharacters(String str){
        // only alphanumeric characters, whitespace or sentence-ending punctuation
        str = str.replaceAll("[^\\p{L}\\s\\p{N}.,?!']", " ");
        // remove single letter/number words that may be floating around
        str = str.replaceAll("(\\s[\\pL\\pN][\\s\\.\\?!,])", " ");
        str = removeExtraSpaces(str);
        return str;
    }

    private static String removeExtraSpacesFromParagraph(String str) {
        return removeExtraSpaces(str)
                .replaceAll("\n+", "\n")
                .replaceAll("(\n )", "\n");
        // TODO: remove extra spaces near punctuation
    }

    // Removes extra spaces from given string. For paragraphs with
    // possible extra line breaks use removeExtraSpacesFromParagraph
    private static String removeExtraSpaces(String str) {
        return str
                .trim()
                .replaceAll(" +", " ");
        // TODO: remove extra spaces near punctuation
    }

    private static String cleanParagraph(String str) {
        return removeExtraSpacesFromParagraph(removeUnwantedCharacters(str));
    }

    private static String cleanLine(String str) {
        return removeExtraSpaces(removeUnwantedCharacters(str));
    }

    public static String cleanFileInMemory(Path filePath) {
        try {
            String fileContent = new String(Files.readAllBytes(filePath), "UTF-8");
            String clean = cleanParagraph(fileContent);
            return clean;
        } catch (IOException e) {
            log.error("Bad filepath {}", filePath);
            return null;
        }
    }

    public static String cleanFileInMemory(String filePath) {
        return cleanFileInMemory(Paths.get(filePath));
    }

    public static Path cleanFileOnDisk(Path filePath) {
        try {
            Scanner in = new Scanner(filePath);
            // First write to a temporary file then move it on the original file
            Path tempPath = Paths.get(System.getProperty("user.dir"), "data", "processed_files",
                    filePath.getFileName().toString().split("\\.")[0] + ".temp");
            if (tempPath.toFile().exists()) {
                Files.delete(tempPath);
            }
            PrintWriter out = new PrintWriter(tempPath.toFile());
            String line;
            while(in.hasNextLine()) {
                line = in.nextLine();
                String cleaned = cleanLine(line);
                if(cleaned.isEmpty()) continue;
                out.println(cleaned);
            }
            in.close();
            out.close();
            return tempPath;
        } catch (IOException e) {
            log.error("Something went wrong cleaning file... Check the input path");
            return null;
        }
    }

    public static Path cleanFileOnDisk(String filePath) {
        return cleanFileOnDisk(Paths.get(filePath));
    }

    public static void main(String[] args) {
//        Cleaner.cleanFileInMemory("data\\corpora\\medium_corpus.txt");
        Cleaner.cleanFileOnDisk(Paths.get("data\\corpora\\short_corpus.txt"));
    }
}
