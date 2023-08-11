package searchengine.services.indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {

    private final LuceneMorphology luceneMorph ;

    public TextParser() throws IOException {
        this.luceneMorph = new RussianLuceneMorphology();
    }

    public HashMap<String, Integer> getLemmas(String text) {
        String clearText = clearHtmlTags(text);
        return parseTextToLemmas(clearText.toLowerCase());
    }

    private String clearHtmlTags(String htmlText) {
        return Jsoup.parse(htmlText).text();
    }

    private HashMap<String, Integer> parseTextToLemmas(String text) {
        HashMap<String, Integer> foundLemmas = new HashMap<>();
        Pattern pattern = Pattern.compile("[А-Яа-яЁё]+");
        Matcher matcher = pattern.matcher(text);

        while(matcher.find()) {
            String word = matcher.group();
            HashSet<String> wordLemmas = getWordLemmas(word);
            for(String lemma : wordLemmas) {
                addToFoundLemmas(lemma, foundLemmas);
            }
        }

        return foundLemmas;
    }

    private HashSet<String> getWordLemmas(String word) {
        List<String> wordMorphValues = luceneMorph.getMorphInfo(word);
        HashSet<String> wordLemmas = new HashSet<>();
        for(String wordMorphValue : wordMorphValues) {
            if (isItServicePartOfSpeech(wordMorphValue)) {
                continue;
            }
            wordLemmas.add(getNormalForm(wordMorphValue));
        }
        return wordLemmas;
    }

    private boolean isItServicePartOfSpeech(String wordMorphValue) {
        return wordMorphValue.matches(".+МЕЖД$")
            || wordMorphValue.matches(".+СОЮЗ$")
            || wordMorphValue.matches(".+ПРЕДЛ$")
            || wordMorphValue.matches(".+ЧАСТ$");
    }

    private String getNormalForm(String wordMorphValue) {
        return wordMorphValue.substring(0, wordMorphValue.indexOf("|"));
    }

    private void addToFoundLemmas(String lemma, HashMap<String, Integer> lemmas) {
        if (!lemmas.containsKey(lemma)) {
            lemmas.put(lemma, 0);
        }
        lemmas.put(lemma, lemmas.get(lemma) + 1);
    }

}