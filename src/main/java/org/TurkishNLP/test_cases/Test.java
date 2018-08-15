package org.TurkishNLP.test_cases;

import org.TurkishNLP.word2vec.Word2VecModel;

public interface Test {
     void run(Word2VecModel m);
     String results();
}
