package searchengine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.lang.Nullable;
import searchengine.services.indexing.TextParser;
import searchengine.services.search.LemmaData;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WebUtils {

    private WebUtils() {}

    public static synchronized @Nullable URL makeUrlFromString(final String string) {
        try {
            return new URL(string);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static String makeStringFromUrl(URL siteUrl) {
        return siteUrl.getProtocol() + "://" + siteUrl.getHost();
    }

    public static String findTitle(String wepPageContent) {
        Document jsoupDocument = Jsoup.parse(wepPageContent);
        return jsoupDocument.title();
    }

    public static String makeSnippet(String webPageContent, List<LemmaData> lemmasData) {
        HashSet<String> queryLemmas = new HashSet<>();
        lemmasData.forEach(l -> queryLemmas.add(l.getLemma()));
        final TextParser textParser = TextParser.makeTextParser();
        if (textParser == null) {
            return "Не удалось сформировать сниппет - не создался объект класса LuceneMorphology";
        }
        String[] wordsOfPage = clearHtmlTags(webPageContent).split("\\s+");
        List<Integer> lemmasIndexes = new ArrayList<>();
        lemmasIndexes.add(0);
        for (int i = 0; i < wordsOfPage.length; i++) {
            if (isThisWordQueryLemma(wordsOfPage[i], queryLemmas, textParser)) {
                lemmasIndexes.add(i);
            }
        }
        lemmasIndexes.add(wordsOfPage.length - 1);
        return doMakeSnippet(wordsOfPage, lemmasIndexes);
    }

    private static boolean isThisWordQueryLemma(String word, HashSet<String> queryLemmas, TextParser textParser) {
        if (!word.matches("[А-Яа-яЁё]+")) {
            return false;
        }
        HashSet<String> wordLemmas = textParser.getWordLemmas(word.toLowerCase());
        for (String wordsLemma : wordLemmas) {
            if (queryLemmas.contains(wordsLemma)) {
                return true;
            }
        }
        return false;
    }

    private static String doMakeSnippet(String[] wordsOfPage, List<Integer> lemmasIndexes) {
        final int snippetLength = 24;
        final int shift = Math.max(3, snippetLength / (lemmasIndexes.size() - 2) / 2);
        StringBuilder snippetBuilder = new StringBuilder();
        int i = 1;
        int wordsCount = 0;
        int leftEdge = lemmasIndexes.get(i - 1);
        while (i < lemmasIndexes.size() - 1 && wordsCount < snippetLength) {
            int startIndex = Math.max(leftEdge, lemmasIndexes.get(i) - shift);
            if (lemmasIndexes.get(i) - shift > leftEdge) {
                snippetBuilder.append(" ...");
            }
            int finishIndex = Math.min(lemmasIndexes.get(i) + shift, lemmasIndexes.get(i + 1) - 1);
            for (int j = startIndex; j <= finishIndex ; j++) {
                if (j == lemmasIndexes.get(i)) {
                    snippetBuilder.append(" <b>" + wordsOfPage[j] + "</b>");
                } else {
                    snippetBuilder.append(" " + wordsOfPage[j]);
                }
            }
            wordsCount += finishIndex - startIndex + 1;
            leftEdge = finishIndex + 1;
            i += 1;
        }
        if (leftEdge + shift < wordsOfPage.length - 1) {
            snippetBuilder.append(" ...");
        }
        return snippetBuilder.toString();
    }

    public static String clearHtmlTags(String htmlText) {
        return Jsoup.parse(htmlText).text();
    }

}