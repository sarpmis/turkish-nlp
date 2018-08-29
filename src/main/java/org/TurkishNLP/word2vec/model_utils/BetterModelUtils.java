package org.TurkishNLP.word2vec.model_utils;

import org.TurkishNLP.word2vec.Word2VecModel;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.reader.impl.BasicModelUtils;
import org.deeplearning4j.models.sequencevectors.sequence.SequenceElement;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.util.*;

/**
 * An adaptation of BasicModelUtils from the DL4J library that can return scores as well as labels
 * from wordsNearest queries. Also normalizes vectors by default
 *
 * Most of the code is reused and reorganized from BasicModelUtils
 * @param <T>
 */
public class BetterModelUtils<T extends SequenceElement> extends BasicModelUtils<T> {

    public List<ScoredLabel> wordsNearestScored(INDArray word, int top) {
        InMemoryLookupTable l = (InMemoryLookupTable) lookupTable;
        INDArray syn0 = l.getSyn0();
        if (!normalized) {
            synchronized (this) {
                if (!normalized) {
                    syn0.diviColumnVector(syn0.norm2(1));
                    normalized = true;
                }
            }
        }
        INDArray weights = syn0.norm2(0).rdivi(1).muli(word);
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
//            System.out.println(sort.getInt(i) + " " + vocabCache.wordAtIndex(sort.getInt(i)) + " : " + sorted[1].getDouble(i));
            ret.add(new ScoredLabel(vocabCache.wordAtIndex(sort.getInt(i)), sorted[1].getDouble(i)));
        }
        return ret;

        /**
         * THIS IS FOR ON-DISK
         */
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
    }

    public List<ScoredLabel> wordsNearestScored(Collection<String> positive, Collection<String> negative, int top) {
        INDArray words = Nd4j.create(lookupTable.layerSize());
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

        @Override
        public String toString() {
            return label + " : " + score;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if(!ScoredLabel.class.isAssignableFrom(obj.getClass())) {
                return false;
            }

            final ScoredLabel other = (ScoredLabel) obj;
            if ((this.label == null) ? (other.label != null) : !this.label.equals(other.label)) {
                return false;
            }

            if (this.score != other.score) {
                return false;
            }

            return true;
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
        Word2VecModel model = Word2VecModel.readModelByName("n_10epoch_250layer_10min_15neg");
        Word2Vec w = model.getWord2Vec();
        InMemoryLookupTable l = (InMemoryLookupTable) w.getLookupTable();
        BetterModelUtils<VocabWord> utils = new BetterModelUtils<>();
        utils.init(l);
//        utils.normalized = true;
        System.out.println(utils.wordsNearestScored(Arrays.asList("paris", "japonya"), Arrays.asList("fransa"), 10));
    }
}
