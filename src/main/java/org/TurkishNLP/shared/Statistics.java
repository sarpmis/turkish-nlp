package org.TurkishNLP.shared;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to get basic statistics of lists of doubles
 */
public class Statistics {
    private final Integer size;
    private final Double min, max, mean, median, logMean, sdv;
    private final Integer ones;

    public Statistics(List<Double> data) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        data.forEach(d -> stats.addValue(d));
        min = stats.getMin();
        max = stats.getMax();
        sdv = stats.getStandardDeviation();
        size = data.size();
        mean = stats.getMean();

        // median logic
        if((size % 2) == 1) {
            median = stats.getSortedValues()[size/2];
        } else {
            median = (stats.getSortedValues()[size/2] + stats.getSortedValues()[size/2 - 1])/2;
        }

        // log values statistics
        List<Double> logs = data.stream().map(val -> Math.log(val)/Math.log(2)).collect(Collectors.toList());
        DescriptiveStatistics logStats = new DescriptiveStatistics();
        logs.forEach(l -> logStats.addValue(l));
        logMean = logStats.getMean();

        // number of elements that == 1
        int tempOnes = 0;
        for(Double d : data) {
            if(d == 1d) tempOnes += 1;
        }
        ones = tempOnes;
    }

    public Integer getSize() {
        return size;
    }

    public Integer getOnesCount() {
        return ones;
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

    public Double getMedian() {
        return median;
    }

    public Double getLogMean() {
        return logMean;
    }

    public Double getStandardDeviation() {
        return sdv;
    }
}
