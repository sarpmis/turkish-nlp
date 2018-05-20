package org.TurkishNLP;

import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

import java.io.IOException;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException
    {
        System.out.println( "Hello World!" );

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

//        String sentence = "Yarın akşam kar yağacak gibi.";
        String sentence = "Kasım ayı 31 gündür.";
        Log.info("Sentence  = " + sentence);
        List<WordAnalysis> analyses = morphology.analyzeSentence(sentence);

        Log.info("Sentence word analysis result:");
        for (WordAnalysis entry : analyses) {
            Log.info("Word = " + entry.getInput());
            for (SingleAnalysis analysis : entry) {
                Log.info(analysis.formatLong());
            }
        }
        SentenceAnalysis result = morphology.disambiguate(sentence, analyses);

        Log.info("\nAfter ambiguity resolution : ");
        result.bestAnalysis().forEach(Log::info);


    }
}
