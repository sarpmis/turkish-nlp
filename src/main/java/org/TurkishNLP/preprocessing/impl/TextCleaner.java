package org.TurkishNLP.preprocessing.impl;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.preprocessing.ParallelizablePreProcessor;
import org.TurkishNLP.preprocessing.PreProcessor;

import javax.xml.soap.Text;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

@Slf4j
public class TextCleaner extends ParallelizablePreProcessor {

    private String removeUnwantedCharacters(String str){
        // only alphanumeric characters, whitespace, sentence-ending punctuation or apostrophe
        str = str.replaceAll("[^\\p{L}\\s\\p{N}.,?!'’]", " ");
        // remove any apostrophe that isn't preceded and followed by a letter
        str = str.replaceAll("['’](?!\\p{L})|(?<!\\p{L})['’]", "");
        // remove single letter/number words that may be floating around
        str = str.replaceAll("(\\s[\\pL\\pN][\\s.?!,])", " ");
        str = removeExtraSpaces(str);
        return str;
    }

    /*
     * Removes extra whitespace from given string, leaving single spaces between words.
     * For paragraphs with possible extra line breaks removeExtraSpacesFromParagraph
     */
    private String removeExtraSpaces(String str) {
        return str
                .trim()
                .replaceAll(" +", " ");
        // TODO: remove extra spaces near punctuation
    }

    private String removeExtraSpacesFromParagraph(String str) {
                // remove more than one whitespace
        return removeExtraSpaces(str)
                // remove more than one line break
                .replaceAll("[\r\n]+", "\n")
                // remove whitespace at beginning of line
                .replaceAll("(\n )", "\n")
                // remove whitespace preceding punctuation
                .replaceAll(" (?=[.,?!])", "");
    }

    private String cleanParagraph(String str) {
        return removeExtraSpacesFromParagraph(removeUnwantedCharacters(str));
    }

    private String cleanLine(String str) {
        return removeExtraSpaces(removeUnwantedCharacters(str));
    }

    public String cleanFileInMemory(Path filePath) {
        try {
            String fileContent = new String(Files.readAllBytes(filePath), "UTF-8");
            String clean = cleanParagraph(fileContent);
            return clean;
        } catch (IOException e) {
            log.error("Bad filepath {}", filePath);
            return null;
        }
    }

    public String cleanFileInMemory(String filePath) {
        return cleanFileInMemory(Paths.get(filePath));
    }

    public void cleanFileOnDisk(File input, File output) throws FileNotFoundException{
        try (
                Scanner in = new Scanner(input);
                PrintWriter out = new PrintWriter(output)
        ) {
            String line;
            while(in.hasNextLine()) {
                line = in.nextLine();
                String cleaned = cleanLine(line);
                if(cleaned.isEmpty()) continue;
                out.println(cleaned);
            }
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Error cleaning file: input or output file not found!");
        }
    }

    @Override
    public boolean processFile(File input, File output) {
        try {
            cleanFileOnDisk(input, output);
            log.info("File [{}] cleaned successfully. Outputted to [{}]", input, output);
            return true;
        } catch(FileNotFoundException e) {
            log.error("Error while cleaning file: input or output file not found!");
            return false;
        }
    }

    @Override
    public String processLine(String input) {
        String processed = removeExtraSpaces(removeUnwantedCharacters(input));
        return processed.isEmpty() ? null : processed;
    }
}
