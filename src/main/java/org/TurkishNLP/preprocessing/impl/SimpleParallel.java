package org.TurkishNLP.preprocessing.impl;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.preprocessing.ParallelizablePreProcessor;

import java.io.File;

/*
 * Dummy class used for testing parallelization
 */
@Slf4j
public class SimpleParallel extends ParallelizablePreProcessor {
    @Override
    public String processLine(String line) {
        return line;
    }

    @Override
    public boolean processFile(File input, File output) {
        return false;
    }
}
