package org.TurkishNLP.word2vec.model_utils;

import com.google.common.collect.Lists;
import lombok.NonNull;
import org.TurkishNLP.word2vec.Word2VecModel;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.reader.impl.BasicModelUtils;
import org.deeplearning4j.models.sequencevectors.sequence.SequenceElement;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.primitives.Counter;
import org.nd4j.linalg.util.MathUtils;
import org.nd4j.util.SetUtils;

import java.io.IOException;
import java.util.*;

/**
 * An adaptation of BasicModelUtils from the DL4J library that can return scores as well as labels
 * from wordsNearest queries. Also normalization is optional and done manually
 *
 * Most of the code is reused and reorganized from BasicModelUtils
 * @param <T>
 */
public class BasicModelUtils2<T extends SequenceElement> extends BasicModelUtils<T> {

    public List<ScoredLabel> wordsNearestScored(INDArray words, int top) {
        InMemoryLookupTable l = (InMemoryLookupTable) lookupTable;
        INDArray syn0 = l.getSyn0();
        if (!normalized) {
            synchronized (this) {
                if (!normalized) {
                    System.out.println(((InMemoryLookupTable) lookupTable).getVocab().numWords());
                    System.out.println(syn0.size(0) + " " + syn0.size(1));
                    syn0.diviColumnVector(syn0.norm2(1));
                    normalized = true;
                }
            }
        }
        INDArray weights = syn0.norm2(0).rdivi(1).muli(words);
        INDArray distances = syn0.mulRowVector(weights).sum(1);
        INDArray[] sorted = Nd4j.sortWithIndices(distances, 0, false);
        INDArray sort = sorted[0];
        List<ScoredLabel> ret = new ArrayList<>();


        if (top > sort.length())
            top = sort.length();
        //there will be a redundant word
        int end = top;
        for (int i = 0; i < end; i++) {
            String add = vocabCache.wordAtIndex(sort.getInt(i));
            if (add == null || add.equals("UNK") || add.equals("STOP")) {
                end++;
                if (end >= sort.length())
                    break;
                continue;
            }
            System.out.println(sort.getInt(i) + " " + vocabCache.wordAtIndex(sort.getInt(i)) + " : " + sorted[1].getDouble(i));
//            ret.add(vocabCache.wordAtIndex(sort.geInt(i)));
        }

//        Counter<String> dist = new Counter<>();
//
//        for (String s : vocabCache.words()) {
//            INDArray otherVec = lookupTable.vector(s);
//            double sim = Transforms.cosineSim(words, otherVec);
//            dist.incrementCount(s, (float) sim);
//        }
//        dist.keepTopNElements(top);
////        System.out.println(dist.keySet());
//        for(String s : dist.keySet()) {
//            System.out.println(s + " : " + dist.getProbability(s));
//        }
        return null;
    }

    public List<ScoredLabel> wordsNearestScored(Collection<String> positive, Collection<String> negative, int top) {
        INDArray words = Nd4j.create(lookupTable.layerSize());
        //    Set<String> union = SetUtils.union(new HashSet<>(positive), new HashSet<>(negative));
        for (String s : positive)
            words.addi(lookupTable.vector(s));


        for (String s : negative)
            words.addi(lookupTable.vector(s).mul(-1));

        return wordsNearestScored(words, top);
    }

    public class ScoredLabel {
        private final String label;
        private final Double score;

        public ScoredLabel(String label, Double score) {
            this.label = label;
            this.score = score;
        }

        public Double getScore() {
            return score;
        }

        public String getLabel() {
            return label;
        }
    }


    public static void main(String[] args) throws IOException{
//        SentenceIterator iter = new BasicLineIterator("data\\corpora\\short_corpus.txt");
//        TokenizerFactory t = new DefaultTokenizerFactory();
//        t.setTokenPreProcessor(new CommonPreprocessor());
//        Word2Vec w = new Word2Vec.Builder()
//                .layerSize(257)
//                .iterate(iter)
//                .tokenizerFactory(t)
//                .build();
//        System.out.println(w.getLayerSize());
//        System.out.println(w.getLookupTable().layerSize());
//        InMemoryLookupTable l = (InMemoryLookupTable) w.getLookupTable();
//        w.fit();
//        System.out.println(l.getSyn0().size(0) + " " + l.getSyn0().size(1));
//        WordVectorSerializer.writeWord2VecModel(w, "data\\models\\temp.model");
//        w = WordVectorSerializer.readWord2VecModel("data\\models\\temp.model");
//        System.out.println(w.getLayerSize());
//        System.out.println(w.getLookupTable().layerSize());
//        l = (InMemoryLookupTable) w.getLookupTable();
//        System.out.println(l.getSyn0().size(0) + " " + l.getSyn0().size(1));
        Word2VecModel m = Word2VecModel.readModelByName("gensim");
        InMemoryLookupTable l = (InMemoryLookupTable) m.getWord2Vec().getLookupTable();
        System.out.println(l.getSyn0().size(0) + " " + l.getSyn0().size(1));

        m = Word2VecModel.readModelByName("gensim_noUNK_parallel_min5");
        l = (InMemoryLookupTable) m.getWord2Vec().getLookupTable();
        System.out.println(l.getSyn0().size(0) + " " + l.getSyn0().size(1));
    }
}
