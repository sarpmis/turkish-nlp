package org.TurkishNLP.test_cases;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

public class Statistics {
    private final Integer size;
    private final Double min, max, mean, sdv;

    public Statistics(List<Double> data) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        data.forEach(d -> stats.addValue(d));
        min = stats.getMin();
        max = stats.getMax();
        mean = stats.getMean();
        sdv = stats.getStandardDeviation();
        size = data.size();
    }

    public Integer getSize() {
        return size;
    }

    public Double getMax() {
        return max;
    }

    public Double getMean() {
        return mean;
    }

    public Double getMin() {
        return min;
    }

    public Double getStandardDeviation() {
        return sdv;
    }
}
