package org.TurkishNLP.word2vec;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.shared.Timer;
import org.TurkishNLP.test_cases.AnalogyTest;
import org.TurkishNLP.test_cases.Test;
import org.TurkishNLP.test_cases.Tester;
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
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
        return w.wordsNearestSum(positive, negative, top);
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
        String modelDirectory = System.getProperty("user.dir") + System.lineSeparator()
                + "data" + System.lineSeparator() + "models" + System.lineSeparator();
        return readModelByPath(modelDirectory + modelName + ".model", modelName);
    }

    public static Word2VecModel readModelByPath(String filePath, String modelName) throws FileNotFoundException {
        File targetFile = new File(filePath);
        if (!targetFile.exists()) {
          log.warn("File [{}] does not exist, aborting", filePath);
          throw new FileNotFoundException(filePath);
        } else {
            // TODO: add timing
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
        if(!output.isFile()) {
            log.warn("[{}] is not a file, aborting", filePath);
            return;
        }
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
        String modelDirectory = System.getProperty("user.dir") + System.lineSeparator()
                + "data" + System.lineSeparator() + "models" + System.lineSeparator();
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

    public static void main(String[] args) throws IOException {
//        Word2VecModel one = Word2VecModel.readModel("test");
//        Word2VecModel two = Word2VecModel.readModel("trimmed_dictionary");
//        System.out.println(one.getVocabCount());
//        System.out.println(one.getClosest("futbol_Noun", 10));
//        System.out.println(two.getVocabCount());
//        System.out.println(two.getClosest("futbol_Noun", 10));
//
        // ********* MODEL TRAINING *********
//        Word2VecParams p = new Word2VecParams("dl4j_batch_size1000")
//                .setLayerSize(400)
////                .setLearningRate(0.025)
//                .setWindowSize(5)
//                .setMinWordFrequency(5)
//                .setSubSampling(0.001)
////                .setMinLearningRate(0.0001)
//                .setLearningAlgortihm(Word2VecParams.LearningAlgorithm.CBOW)
//                .setNegativeSampling((double)5)
//                .setNumEpochs(5)
//                .setBatchSize(10000)
//                .setHierarchicSoftmax(false);
////                .setCorpusPath("data\\corpora\\gensim.txt")
////                .setDictionaryPath("data\\corpora\\gensim.txt");
//
//        Word2VecModel m = Word2VecModel.initializeWithParams(p);
//        Word2VecTrainer.trainModel(m.getWord2Vec(), Paths.get("data\\corpora\\gensim_no_punc.txt"));
//        Word2VecModel.saveModel(m);

        Word2Vec w = new Word2Vec.Builder()
                .elementsLearningAlgorithm(new CBOW<>())
                .epochs(5)
                .useHierarchicSoftmax(false)
                .negativeSample((double) 5)
                .minWordFrequency(5)
                .layerSize(400)
                .sampling(0.001)
                .iterate(new BasicLineIterator("data\\corpora\\gensim_no_punc.txt"))
                .tokenizerFactory(new DefaultTokenizerFactory())
                .build();

        w.fit();
//        System.out.println(m.getClosest(Arrays.asList("kral", "kadın"), Arrays.asList("erkek"), 20));
        WordVectorSerializer.writeWord2VecModel(w, "data\\models\\dl4j.model");

        // ********** TESTING **********
        PrintWriter out = new PrintWriter("data\\testing\\gensim_vectors.txt");
        List<String> models = Arrays.asList("dl4j2", "gensim2", "dl4j_batch_size1000");
        Tester t = new Tester();
        List<Test> tests = new ArrayList<>();
        tests.add(new AnalogyTest("fransa", "paris", "roma", "italya", 20));
        tests.add(new AnalogyTest("fransa", "paris", "londra", "ingiltere", 20));
        tests.add(new AnalogyTest("fransa", "paris", "budapeşte", "macaristan", 20));
        tests.add(new AnalogyTest("macaristan", "budapeşte", "bükreş", "romanya", 20));
        tests.add(new AnalogyTest("macaristan", "budapeşte", "beijing", "çin", 20));
        tests.add(new AnalogyTest("kral", "erkek", "kadın", "kraliçe", 20));
        for(String modelName : models) {
            Word2VecModel mo = Word2VecModel.readModelByName(modelName);
            t.runTestsOnModel(mo, tests, out);
        }
        out.close();
    }
}
