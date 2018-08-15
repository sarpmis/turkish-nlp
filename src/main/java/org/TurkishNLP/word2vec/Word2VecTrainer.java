package org.TurkishNLP.word2vec;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

@Slf4j
public class Word2VecTrainer {

    public static void trainModel(Word2Vec model, String trainingFilePath){
        trainModel(model, Paths.get(trainingFilePath));
    }

    public static void trainModel(Word2Vec model, Path trainingFilePath){
        try {
            File trainingFile = trainingFilePath.toFile();
            SentenceIterator iterator = new BasicLineIterator(trainingFile);
            TokenizerFactory tokenizer = new DefaultTokenizerFactory();
            trainModel(model, iterator, tokenizer);
        } catch (IOException e) {
            log.error("Training file not found...");
        }
    }

    /*
     * Trains existing Word2Vec model with given iterator and tokenizer.
     * @pre VocabCache and LookupTable must be set for the model.
     */
    public static void trainModel(Word2Vec model, SentenceIterator iterator, TokenizerFactory tokenizer){
        model.setTokenizerFactory(tokenizer);
        model.setSentenceIterator(iterator);
        log.info("Fitting Word2Vec model");
        model.fit();
    }
}
