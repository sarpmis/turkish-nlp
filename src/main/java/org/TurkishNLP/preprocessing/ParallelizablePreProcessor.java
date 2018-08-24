package org.TurkishNLP.preprocessing;

public abstract class ParallelizablePreProcessor extends PreProcessor {

    /*
     * Function to be called by {@link ParallelPreProcessor} to process
     * a line of input
     * If null is returned the writer will not print anything for this
     * line.
     */
    public abstract String processLine(String input);

}
