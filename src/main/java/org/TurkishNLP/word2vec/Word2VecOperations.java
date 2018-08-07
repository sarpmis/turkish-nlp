package org.TurkishNLP.word2vec;

import lombok.extern.slf4j.Slf4j;
import org.TurkishNLP.shared.Timer;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

@Slf4j
public class Word2VecOperations {

    public static Word2Vec readModel(String modelName){
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
            return w;
        }
    }

    public static void saveModel(Word2Vec model, String modelName){
        Path outPath = Paths.get(System.getProperty("user.dir"), "data", "models" ,modelName + ".model");
        if (outPath.toFile().exists()) {
            log.info("A processed file " + outPath + " already exists, aborting");
            return;
        } else {
            log.info("Saving model '" + modelName + "' to disk...");
            Timer.setTimer();
            WordVectorSerializer.writeWord2VecModel(model, outPath.toString());
            Timer.endTimer();
            log.info("Finished saving model in " + Timer.results());
        }
    }

    public static void printClosest(Word2Vec model, Collection<String> lst, int top){
        for(String s : lst){
            printClosest(model, s, top);
        }
    }

    public static void printClosest(Word2Vec model, String word, int top){
        Collection<String> closest = model.wordsNearest(word, top);
        log.info("Closest words to" + "'" + word + "' : " + closest);
    }
}
