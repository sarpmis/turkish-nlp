package org.TurkishNLP.test_cases;

import org.TurkishNLP.word2vec.Word2VecModel;

public interface Test {
     void run(Word2VecModel m);
     String results();

     // test tags
     static final String SIMILARITY_TEST_TAG = "S";
     static final String ANALOGY_TEST_TAG = "A";

     /*
      * Reads a test from a line in the text file containing hand-written tests
      *
      * @param s the string to parse
      * @return the appropriate test object
      * @pre s must start with an appropriate tag followed by the # character and contain
      *     the arguments for the test in space separated format
      *           */
     static Test parseTest(String s) {
          String[] arr  = s.split("#");
          if(arr.length != 2) return null;

          String[] args = arr[1].trim().split(" ");

          switch (arr[0]) {
               case SIMILARITY_TEST_TAG:
                   if(args.length != 2) return null;
                   try {
                       return new SimilarityTest(args[0], Integer.parseInt(args[1]));
                   } catch(NumberFormatException e) {
                       return null;
                   }
               case ANALOGY_TEST_TAG:
                   if(args.length != 5) return null;
                   try {
                       return new AnalogyTest(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]));
                   } catch(NumberFormatException e) {
                       return null;
                   }
               default:
                   return null;
          }
     }
}
