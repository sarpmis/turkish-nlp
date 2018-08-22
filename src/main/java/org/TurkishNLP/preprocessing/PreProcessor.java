package org.TurkishNLP.preprocessing;

import java.io.File;
import java.nio.file.Path;

public abstract class PreProcessor {
    /*
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
