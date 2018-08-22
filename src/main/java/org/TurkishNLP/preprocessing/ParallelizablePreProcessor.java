package org.TurkishNLP.preprocessing;

import java.util.concurrent.atomic.AtomicLong;

public abstract class ParallelizablePreProcessor extends PreProcessor {

    public abstract String processLine(String input);

}
