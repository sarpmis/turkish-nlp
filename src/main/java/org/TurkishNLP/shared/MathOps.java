package org.TurkishNLP.shared;

public class MathOps {
    public static Double roundDoubleTo(Double d, int to) throws ArithmeticException {
        if(to < 0) throw new ArithmeticException("Can't round to negative decimal points");
        long mulVal = (long) Math.pow(10d, to);
        return Math.floor(d * mulVal) / mulVal;
    }
}
