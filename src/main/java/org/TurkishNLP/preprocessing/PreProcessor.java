package org.TurkishNLP.preprocessing;

import java.io.File;
import java.nio.file.Path;

public abstract class PreProcessor {

    /*
     * Input is read from the input file and written to the output file
     *
     * @param input the file to read input from
     * @param output the destination to write processed file
     * @return processing succeeded
     */
    public abstract boolean processFile(File input, File output);

    public boolean processFile(String inputPath, String outputPath) {
        return this.processFile(new File(inputPath), new File(outputPath));
    }

    public boolean processFile(Path inputPath, Path outputPath) {
        return this.processFile(inputPath.toFile(), outputPath.toFile());
    }
}
