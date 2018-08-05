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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

@Slf4j
public class Word2VecInitializer {

    public static Word2Vec initializeModel(@NonNull String pathToDict) {
        log.info("Initializing Word2Vec model...");

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


    public static void main(String[] args) throws Exception {
//        Word2Vec w = Word2VecInitializer.initializeModel(Paths.get(System.getProperty("user.dir"),
//                "data", "processed_files", "dictionary.processed").toString());
//
//        Collection<String> lst = w.wordsNearest("tarih_Noun", 10);
//        log.info("Closest words to 'tarih' before: " + lst);
//
//        Word2VecTrainer.trainModel(w, Paths.get(System.getProperty("user.dir"), "data",
//                "processed_files","corpuswiki.processed").toString());
//
//        lst = w.wordsNearest("tarih_Noun", 10);
//        log.info("Closest words to 'tarih' : " + lst);
//        Word2VecOperations.saveModel(w, "test");

        Word2Vec w = Word2VecOperations.readModel("test");
        Collection<String> lst = Arrays.asList("futbol_Noun",
                "Cumhuriyet_Noun_Prop",
                "komutan_Noun",
                "iyi_Adj",
                "Tayyip_Noun_Prop",
                "İzmir_Noun_Prop"
        );
        Word2VecOperations.printClosest(w, lst, 10);
    }
}
