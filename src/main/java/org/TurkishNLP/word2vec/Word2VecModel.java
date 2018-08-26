package org.TurkishNLP.word2vec;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.learning.ElementsLearningAlgorithm;
import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW;
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper that holds a Word2Vec and some extra info
 * Intended to ultimately be the class to use when doing
 * all w2v operations
 */
@Slf4j
public class Word2VecModel {
    private String modelName;
    private Word2Vec w;
    private VectorsConfiguration config;

    public Word2VecModel(@NonNull Word2Vec w, @NonNull String modelName) {
        this.w = w;
        this.config = w.getConfiguration();
        this.modelName = modelName;
    }

    public int getVocabCount() {
        return w.getVocab().numWords();
    }

    public VocabCache getVocab() {
        return w.getVocab();
    }

    public Word2Vec getWord2Vec() {
        return w;
    }

    public String getName() {
        return modelName;
    }

    //***************** WORD OPERATIONS *****************
    public Collection<String> getClosest(@NonNull String word, int top) {
        return w.wordsNearest(word, top);
    }

    public Collection<String> getClosest(List<String> positive, List<String> negative, int top) {
        try {
            return w.wordsNearestSum(positive, negative, top);
        } catch(NullPointerException e) {
            List<String> temp = new ArrayList<>();
            temp.addAll(positive);
            temp.addAll(negative);
            VocabCache v = getVocab();
            temp = temp.stream().filter(i -> !v.containsWord(i)).collect(Collectors.toList());
            log.error("Words: {} don't appear in the vocabulary", temp);
            return null;
        }
    }

    public double getSimilarity(@NonNull  String word1, @NonNull String word2) {
        return w.similarity(word1, word2);
    }

    //***************** INITIALIZER *****************
    public static Word2VecModel initializeWithParams(@NonNull Word2VecParams p) throws IllegalArgumentException {
        log.info("Initializing model with params");

        // Set the learning algorithm
        ElementsLearningAlgorithm<VocabWord> algorithm = null;
        switch(p.getLearningAlgorithm()) {
            case CBOW:
                algorithm = new CBOW<>();
                break;
            case SKIP_GRAM:
                algorithm = new SkipGram<>();
                break;
        }

        // set hyperparameters
        Word2Vec.Builder b = new Word2Vec.Builder()
                .resetModel(false)
                .epochs(p.getNumEpochs())
                .batchSize(p.getBatchSize())
                .iterations(p.getIterations())
                .learningRate(p.getLearningRate())
                .minLearningRate(p.getMinLearningRate())
                .sampling(p.getSubSampling())
                .windowSize(p.getWindowSize())
                .elementsLearningAlgorithm(algorithm)
                .minWordFrequency(p.getMinWordFrequency())
                .negativeSample(p.getNegativeSampling())
                .useHierarchicSoftmax(p.getHierarchicSoftmax());

        // If dictionary path is specified then we create the dictionary
        // from that file instead of the corpus during training
        String dictPath = p.getDictionaryPath();
        if(dictPath != null) {
            // create dictionary iterator
            File dictFile = new File(dictPath);
            SentenceIterator iterator;
            try {
                iterator = new BasicLineIterator(dictFile);
            } catch (IOException e) {
                log.error("The dictionary path provided does not exist");
                throw new IllegalArgumentException();
            }

            log.info("Building vocabulary from dictionary at [{}]", dictPath);

            AbstractCache<VocabWord> vocabCache = new AbstractCache.Builder<VocabWord>().build();

            TokenizerFactory tokenizer = new DefaultTokenizerFactory();

            SentenceTransformer transformer = new SentenceTransformer.Builder()
                    .iterator(iterator)
                    .tokenizerFactory(tokenizer)
                    .build();

            AbstractSequenceIterator<VocabWord> sequenceIterator =
                    new AbstractSequenceIterator.Builder<>(transformer).build();

            // if dictionary trimming was done beforehand min word frequency should be set as 1
            VocabConstructor<VocabWord> constructor = new VocabConstructor.Builder<VocabWord>()
                    .addSource(sequenceIterator, p.getMinWordFrequency())
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

            b = b.vocabCache(vocabCache)
                    .lookupTable(lookupTable);
        }

        log.info("Creating word2vec...");
        Word2Vec w = b.build();

        Word2VecModel mod = new Word2VecModel(w, p.getName());
        log.info("Model initialization complete!");
        return mod;
    }

    //***************** MODEL READ/WRITE *****************
    /**
     * Reads a Word2Vec file and returns a Word2VecModel for it
     */
    public static Word2VecModel readModelByName(String modelName) throws FileNotFoundException{
        String modelDirectory = System.getProperty("user.dir") + File.separator
                + "data" + File.separator + "models" + File.separator;
        return readModelByPath(modelDirectory + modelName + ".model", modelName);
    }

    public static Word2VecModel readModelByPath(String filePath, String modelName) throws FileNotFoundException {
        File targetFile = new File(filePath);
        if (!targetFile.exists()) {
          log.warn("File [{}] does not exist, aborting", filePath);
          throw new FileNotFoundException(filePath);
        } else {
            Word2Vec w = WordVectorSerializer.readWord2VecModel(filePath);
            return new Word2VecModel(w, modelName);
        }
    }

    /**
     * Saves a Word2Vec to disk
     *
     * @param model model that contains the Word2Vec
     * @param filePath destination to save
     * @param override if set to true and a file already exists at filePath replaces it
     */
    public static void saveModel(@NonNull Word2VecModel model, String filePath, boolean override) {
        if(model == null) {
            log.warn("Cannot write null model to file");
            return;
        }
        File output = new File(filePath);
        if(override && output.exists()) {
            log.warn("The file [{}] already exists, aborting", filePath);
            return;
        }
        // TODO: add timing
        WordVectorSerializer.writeWord2VecModel(model.getWord2Vec(), filePath);
        log.info("Finished saving model [{}] to [{}]", model.getName(), filePath);
    }

    /**
     * Saves a given Word2VecModel instance to the designated folder for models
     */
    public static void saveModel(@NonNull Word2VecModel model, boolean override) {
        String modelDirectory = System.getProperty("user.dir") + File.separator
                + "data" + File.separator + "models" + File.separator;
        // FIXME: this filepath ^ should be a constant somewhere
        saveModel(model, modelDirectory + model.getName() + ".model", override);
    }


    //***************** MODEL TRAINING *****************
    public static void trainModel(@NonNull Word2Vec model, File trainingFile) {
        try {
            SentenceIterator iterator = new BasicLineIterator(trainingFile);
            TokenizerFactory tokenizer = new DefaultTokenizerFactory();
            model.setTokenizerFactory(tokenizer);
            model.setSentenceIterator(iterator);
            log.info("Starting training using file [{}]", trainingFile);
            model.fit();
        } catch (FileNotFoundException e) {
            log.error("Training file [{}] not found", trainingFile);
        }
    }

    public static void trainModel(@NonNull Word2Vec model, String trainingFilePath){
        trainModel(model, Paths.get(trainingFilePath));
    }

    public static void trainModel(@NonNull Word2Vec model, Path trainingFilePath){
        trainModel(model, trainingFilePath.toFile());
    }
}
