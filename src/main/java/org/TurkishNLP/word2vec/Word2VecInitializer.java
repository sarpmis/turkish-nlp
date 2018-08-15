package org.TurkishNLP.word2vec;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.sequencevectors.iterators.AbstractSequenceIterator;
import org.deeplearning4j.models.sequencevectors.transformers.impl.SentenceTransformer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabConstructor;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

@Slf4j
public class Word2VecInitializer {
    // DEFAULT HYPERPARAMETERS
    public static final Double DEFAULT_LEARNING_RATE = new Double(0.025D);
    public static final Integer DEFAULT_LAYER_SIZE = new Integer(150);
    public static final Integer DEFAULT_EPOCHS = new Integer(1);
    // TODO: add more hyperparams


    public static Word2Vec initializeModel(@NonNull String pathToDict) {
        log.info("Initializing Word2Vec model...");

        // create iterator for dictionary
        File dictFile = Paths.get(pathToDict).toFile();
        SentenceIterator iterator;
        try { iterator = new BasicLineIterator(dictFile); }
        catch (IOException e){
            log.error("The dictionary file path provided does not exist.");
            return null;
        }

        log.info("Building vocabulary...");
        AbstractCache<VocabWord> vocabCache = new AbstractCache.Builder<VocabWord>().build();

        TokenizerFactory tokenizer = new DefaultTokenizerFactory();

        SentenceTransformer transformer = new SentenceTransformer.Builder()
                .iterator(iterator)
                .tokenizerFactory(tokenizer)
                .build();

        AbstractSequenceIterator<VocabWord> sequenceIterator =
                new AbstractSequenceIterator.Builder<>(transformer).build();

        VocabConstructor<VocabWord> constructor = new VocabConstructor.Builder<VocabWord>()
                .addSource(sequenceIterator, 1)
                .setTargetVocabCache(vocabCache)
                .build();

        constructor.buildJointVocabulary(false, true);

        log.info("Creating lookup table...");
        WeightLookupTable<VocabWord> lookupTable = new InMemoryLookupTable.Builder<VocabWord>()
                .vectorLength(150)
                .useAdaGrad(false)
                .cache(vocabCache)
                .build();

        lookupTable.resetWeights(true);

        log.info("Creating model...");
        Word2Vec vec = new Word2Vec.Builder()
                .vocabCache(vocabCache)
                .lookupTable(lookupTable)
                .resetModel(false)
                .build();

        log.info("Model initialization complete!");
        return vec;
    }

    /*
     * Builds a Word2Vec model with given hyperparameters or defaults if hyperparams not
     * present.
     * @return a new Word2Vec model
     */
    public static Word2Vec initializeModel(@NonNull String pathToDict,
                                           @Nullable Double learningRate,
                                           @Nullable Integer layerSize,
                                           @Nullable Integer numEpochs) throws IOException {
        log.info("Initializing Word2Vec model...");

        // create iterator for dictionary
        File dictFile = Paths.get(pathToDict).toFile();
        SentenceIterator iterator = new BasicLineIterator(dictFile);

        log.info("Building vocabulary...");
        AbstractCache<VocabWord> vocabCache = new AbstractCache.Builder<VocabWord>().build();

        TokenizerFactory tokenizer = new DefaultTokenizerFactory();

        SentenceTransformer transformer = new SentenceTransformer.Builder()
                .iterator(iterator)
                .tokenizerFactory(tokenizer)
                .build();

        AbstractSequenceIterator<VocabWord> sequenceIterator =
                new AbstractSequenceIterator.Builder<>(transformer).build();

        VocabConstructor<VocabWord> constructor = new VocabConstructor.Builder<VocabWord>()
                .addSource(sequenceIterator, 1)
                .setTargetVocabCache(vocabCache)
                .build();

        constructor.buildJointVocabulary(false, true);

        // Create weight lookup table, set layer size if specified
        log.info("Creating lookup table...");
        InMemoryLookupTable.Builder<VocabWord> wltBuilder= new InMemoryLookupTable.Builder<VocabWord>()
                .useAdaGrad(false)
                .cache(vocabCache);

        if(layerSize !=  null) {
            wltBuilder = wltBuilder.vectorLength(layerSize);
            log.info("Layer size specified as {}...", layerSize);
        }
        else {
            wltBuilder = wltBuilder.vectorLength(DEFAULT_LAYER_SIZE);
            log.info("No layer size specified. Using default value of {}...", DEFAULT_LAYER_SIZE);
        }

        WeightLookupTable<VocabWord> lookupTable = wltBuilder.build();

        // we want a clean table
        lookupTable.resetWeights(true);

        log.info("Creating model...");
        Word2Vec.Builder wb = new Word2Vec.Builder();
        wb = wb.vocabCache(vocabCache)
                .lookupTable(lookupTable)
                .resetModel(false);

        if(learningRate != null) {
            log.info("Learning rate specified as {}...", learningRate);
            wb = wb.learningRate(learningRate);
        } else {
            log.info("No learning rate specified, using default value of {}...", DEFAULT_LEARNING_RATE);
        }
        if(numEpochs != null) {
            log.info("Number of epochs specified as {}...", numEpochs);
            wb.epochs(numEpochs);
        } else {
            log.info("Number of epochs not specified, using default value of {}...", DEFAULT_EPOCHS);
        }

        Word2Vec vec = wb.build();

        log.info("Model initialization complete!");
        return vec;
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

        log.info("Creating model...");
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

    public static void main(String[] args) throws Exception {
        Word2Vec w = Word2VecInitializer.initializeModel(Paths.get(System.getProperty("user.dir"),
                "data", "dictionaries", "remaining.dict").toString());

        Collection<String> lst = w.wordsNearest("tarih_Noun", 10);
        log.info("Closest words to 'tarih' before: " + lst);

        Word2VecTrainer.trainModel(w, Paths.get(System  .getProperty("user.dir"), "data",
                "processed_files","trwiki_corpus.processed").toString());

        lst = w.wordsNearest("tarih_Noun", 10);
        log.info("Closest words to 'tarih' : " + lst);
        Word2VecOperations.saveModel(w, "trimmed_dictionary");

//        Word2Vec w = Word2VecOperations.readModel("test");
//        Collection<String> lst = Arrays.asList("futbol_Noun",
//                "Cumhuriyet_Noun_Prop",
//                "komutan_Noun",
//                "iyi_Adj",
//                "Tayyip_Noun_Prop",
//                "İzmir_Noun_Prop"
//        );
//        Word2VecOperations.printClosest(w, lst, 10);
    }
}
