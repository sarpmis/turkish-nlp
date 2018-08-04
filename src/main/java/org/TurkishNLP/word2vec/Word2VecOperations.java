package org.TurkishNLP.word2vec;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.nio.file.Paths;
import java.util.Collection;

@Slf4j
public class Word2VecOperations {

    public static Word2Vec readModel(String modelName){
        log.info("Reading model '" + modelName + "'...");
        return WordVectorSerializer.readWord2VecModel(
                Paths.get(System.getProperty("user.dir"), "data", "models" ,modelName + ".model").toString());
    }

    public static void saveModel(Word2Vec model, String modelName){
        log.info("Saving model '" + modelName + "' to disk...");
        WordVectorSerializer.writeWord2VecModel(model,
                Paths.get(System.getProperty("user.dir"), "data", "models" ,modelName + ".model").toString());
    }

    public static void updateVocab(){
    }

    public static void printClosest(Word2Vec model, Collection<String> lst, int top){
        for(String s : lst){
            Collection<String> closest = model.wordsNearest(s, top);
            log.info("Closest words to" + "'" + s + "' : " + closest);
        }
    }
}
