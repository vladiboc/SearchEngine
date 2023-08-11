package searchengine.services.indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class TestMorph {
/*************
 *
    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("постоянно");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("некоторых");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("ха");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("или");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("про");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("бы");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("в");
        wordBaseForms.forEach(System.out::println);
        wordBaseForms =
                luceneMorph.getMorphInfo("что");
        wordBaseForms.forEach(System.out::println);

        String text = "Повторное появление леопарда в Осетии позволяет предположить,\n" +
                "что леопард постоянно обитает в некоторых районах Северного\n" +
                "Кавказа.";
        TextParser textParser = new TextParser();
        HashMap<String, Integer> lemmas = textParser.getLemmas(text);
        for (String lemma : lemmas.keySet()) {
            System.out.println(lemma + " : " + lemmas.get(lemma));
        }

    }
 ***********/
}
