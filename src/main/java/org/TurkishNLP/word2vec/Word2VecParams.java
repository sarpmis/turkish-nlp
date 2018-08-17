package org.TurkishNLP.word2vec;

import lombok.NonNull;

import java.nio.file.Path;

public class Word2VecParams {
    // These default values are the defaults that DL4J uses
    public static final Double DEFAULT_LEARNING_RATE = new Double(0.025D);
    public static final Double DEFAULT_MIN_LEARNING_RATE = new Double(0.0001);
    public static final Double DEFAULT_SUB_SAMPLING = new Double(0);
    public static final Integer DEFAULT_LAYER_SIZE = new Integer(150);
    public static final Integer DEFAULT_EPOCHS = new Integer(1);
    public static final Integer DEFAULT_WINDOW_SIZE = new Integer(5);
    public static final Integer DEFAULT_BATCH_SIZE = new Integer(512);
    public static final Integer DEFAULT_ITERATIONS = new Integer(1);


    private String name = null;
    private Path dictionaryPath = null;
    private Path corpusPath = null;
    private Double learningRate = null;
    private Double minLearningRate = null;
    private Double subSampling = null;
    private Integer layerSize = null;
    private Integer numEpochs = null;
    private Integer windowSize = null;
    private Integer batchSize = null;
    private Integer iterations = null;

    public Word2VecParams(@NonNull String name) {
        this.name = name;
    }

    public Word2VecParams setName(String name){
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public Word2VecParams setDictionaryPath(Path dictionaryPath) {
        this.dictionaryPath = dictionaryPath;
        return this;
    }

    public Word2VecParams setCorpusPath(Path corpusPath) {
        this.corpusPath = corpusPath;
        return this;
    }

    public Path getDictionaryPath() {
        if(dictionaryPath == null) {
            throw new NullPointerException("dictionary path not specified!");
        } else {
            return dictionaryPath;
        }
    }

    public Path getCorpusPath() {
        if(corpusPath == null) {
            throw new NullPointerException("corpus path is not specified!");
        } else {
            return corpusPath;
        }
    }

    // HYPERPARAMETER SETTING
    public Word2VecParams setLearningRate(Double learningRate) {
        this.learningRate = learningRate;
        return this;
    }

    public Word2VecParams setMinLearningRate(Double minLearningRate) {
        this.minLearningRate = minLearningRate;
        return this;
    }

    public Word2VecParams setSubSampling(Double subSampling) {
        this.subSampling = subSampling;
        return this;
    }

    public Word2VecParams setLayerSize(Integer layerSize) {
        this.layerSize = layerSize;
        return this;
    }

    public Word2VecParams setNumEpochs(Integer numEpochs) {
        this.numEpochs = numEpochs;
        return this;
    }

    public Word2VecParams setWindowSize(Integer windowSize) {
        this.windowSize = windowSize;
        return this;
    }

    public Word2VecParams setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public Word2VecParams setIterations(Integer iterations) {
        this.iterations = iterations;
        return this;
    }

    // HYPERPARAM GETTING
    public Double getLearningRate() {
        return learningRate == null ? DEFAULT_LEARNING_RATE : learningRate;
    }

    public Double getMinLearningRate() {
        return minLearningRate == null ? DEFAULT_MIN_LEARNING_RATE : minLearningRate;
    }

    public Double getSubSampling() {
        return subSampling == null ? DEFAULT_SUB_SAMPLING : subSampling;
    }

    public Integer getLayerSize() {
        return layerSize == null ? DEFAULT_LAYER_SIZE : layerSize;
    }

    public Integer getNumEpochs() {
        return numEpochs == null ? DEFAULT_EPOCHS : numEpochs;
    }

    public Integer getWindowSize() {
        return windowSize == null ? DEFAULT_WINDOW_SIZE : windowSize;
    }

    public Integer getBatchSize() {
        return batchSize == null ? DEFAULT_BATCH_SIZE : batchSize;
    }

    public Integer getIterations() {
        return iterations == null ? DEFAULT_ITERATIONS : iterations;
    }
}