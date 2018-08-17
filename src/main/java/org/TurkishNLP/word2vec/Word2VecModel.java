package org.TurkishNLP.word2vec;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.shared.Timer;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.sequencevectors.iterators.AbstractSequenceIterator;
import org.deeplearning4j.models.sequencevectors.transformers.impl.SentenceTransformer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.models.word2vec.wordstore.VocabConstructor;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

/*
 * A wrapper that holds a Word2Vec and some extra info
 * Intended to ultimately be the interface to use when doing
 * all w2v operations
 */
@Slf4j
public class Word2VecModel {
    private String modelName;
    private Word2Vec w;
    private VectorsConfiguration config;
    private VocabCache vocab;

    public Word2VecModel(Word2Vec w, String modelName) {
        this.w = w;
        this.config = w.getConfiguration();
        this.modelName = modelName;
        this.vocab = w.getVocab();
    }

    public int getVocabCount() {
        return vocab.numWords();
    }

    public VocabCache getVocab() {
        return vocab;
    }

    public Word2Vec getWord2Vec() {
        return w;
    }

    public String getModelName() {
        return modelName;
    }

    // WORD OPERATIONS
    public Collection<String> getClosest(@NonNull String word, @NonNull int top) {
        return w.wordsNearest(word, top);
    }

    public Collection<String> getClosest(List<String> positive, List<String> negative, int top) {
        return w.wordsNearestSum(positive, negative, top);
    }
    public double getSimilarity(@NonNull  String word1, @NonNull String word2) {
        return w.similarity(word1, word2);
    }


    /*
     * Reads a Word2Vec file and returns a Word2VecModel for it
     */
    public static Word2VecModel readModel(@NonNull String modelName){
        Path readPath = Paths.get(System.getProperty("user.dir"), "data", "models" ,modelName + ".model");
        if (!readPath.toFile().exists()) {
            log.error("Model " + modelName + " does not exist! Aborting...");
            return null;
        } else {
            log.info("Reading model '" + modelName + "'...");
            Timer.setTimer();
            Word2Vec w = WordVectorSerializer.readWord2VecModel(readPath.toString());
            Timer.endTimer();
            log.info("Finished reading model in " + Timer.results());
            return new Word2VecModel(w, modelName);
        }
    }

    public static void saveModel(Word2VecModel model){
        Path outPath = Paths.get(System.getProperty("user.dir"), "data", "models" , model.getModelName() + ".model");
        if (outPath.toFile().exists()) {
            log.info("A processed file " + outPath + " already exists, aborting");
            return;
        } else {
            log.info("Saving model '" + model.getModelName() + "' to disk...");
            Timer.setTimer();
            WordVectorSerializer.writeWord2VecModel(model.getWord2Vec(), outPath.toString());
            Timer.endTimer();
            log.info("Finished saving model in " + Timer.results());
        }
    }

    public static Word2VecModel initializeWithParams(@NonNull Word2VecParams p) {
        log.info("Initializing model with params");

        // create dictionary iterator
        File dictFile = p.getDictionaryPath().toFile();
        SentenceIterator iterator;
        try {
            iterator = new BasicLineIterator(dictFile);
        } catch (IOException e) {
            log.error("The dictionary path provided does not exist");
            return null;
        }

        log.info("Building vocabulary from dictionary...");

        AbstractCache<VocabWord> vocabCache = new AbstractCache.Builder<VocabWord>().build();

        TokenizerFactory tokenizer = new DefaultTokenizerFactory();

        SentenceTransformer transformer = new SentenceTransformer.Builder()
                .iterator(iterator)
                .tokenizerFactory(tokenizer)
                .build();

        AbstractSequenceIterator<VocabWord> sequenceIterator =
                new AbstractSequenceIterator.Builder<>(transformer).build();

        // min element frequency is 1 since we use fixed dictionary. dictionary trimming
        // is done during preprocessing
        VocabConstructor<VocabWord> constructor = new VocabConstructor.Builder<VocabWord>()
                .addSource(sequenceIterator, 1)
                .setTargetVocabCache(vocabCache)
                .build();

        constructor.buildJointVocabulary(false, true);

        log.info("Creating lookup table...");
        WeightLookupTable<VocabWord> lookupTable = new InMemoryLookupTable.Builder<VocabWord>()
                .vectorLength(p.getLayerSize())
                .useAdaGrad(false)
                .cache(vocabCache)
                .build();

        lookupTable.resetWeights(true);

        log.info("Creating word2vec...");
        Word2Vec vec = new Word2Vec.Builder()
                .vocabCache(vocabCache)
                .lookupTable(lookupTable)
                .resetModel(false)
                .epochs(p.getNumEpochs())
                .batchSize(p.getBatchSize())
                .iterations(p.getIterations())
                .learningRate(p.getLearningRate())
                .minLearningRate(p.getMinLearningRate())
                .sampling(p.getSubSampling())
                .windowSize(p.getWindowSize())
                .build();

        Word2VecModel mod = new Word2VecModel(vec, p.getName());
        log.info("Model initialization complete!");
        return mod;
    }

    public String toString() {
        return modelName;
    }

    public static void main(String[] args) {
        Word2VecModel one = Word2VecModel.readModel("test");
        Word2VecModel two = Word2VecModel.readModel("trimmed_dictionary");
        System.out.println(one.getVocabCount());
        System.out.println(one.getClosest("futbol_Noun", 10));
        System.out.println(two.getVocabCount());
        System.out.println(two.getClosest("futbol_Noun", 10));
    }
}
