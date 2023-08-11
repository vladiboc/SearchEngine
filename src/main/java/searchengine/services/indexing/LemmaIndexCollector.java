package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import searchengine.model.DbConnection;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.SearchIndex;
import searchengine.model.entities.SiteLemma;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class LemmaIndexCollector {
    private IndexedPage page;
    private DbConnection dbConnection;

    public void collectAndUpdateLemmasAndIndex() {
        TextParser textParser;
        try {
            textParser = new TextParser();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        HashMap<String, Integer> pageLemmas = textParser.getLemmas(page.getContent());
        updateSiteLemmasAndIndex(pageLemmas);
    }

    private void updateSiteLemmasAndIndex(HashMap<String, Integer> pageLemmas) {
        HashSet<String> pageLemmasStrings = new HashSet<>();
        pageLemmas.forEach((lemma, frequency) -> pageLemmasStrings.add(lemma));
        List<SiteLemma> siteLemmas = dbConnection.getAllLemmasForSite(page.getSite());
        List<SiteLemma> anotherPagesLemmas = new ArrayList<>();
        for (SiteLemma lemma : siteLemmas) {
            String stringLemma = lemma.getLemma();
            if (pageLemmasStrings.contains(stringLemma)) {
                lemma.setFrequency(lemma.getFrequency() + 1);
                pageLemmasStrings.remove(stringLemma);
            } else {
                anotherPagesLemmas.add(lemma);
            }
        }
        anotherPagesLemmas.forEach(lemma -> siteLemmas.remove(lemma));
        for (String lemma : pageLemmasStrings) {
            SiteLemma newLemma = new SiteLemma();
            newLemma.setSite(page.getSite());
            newLemma.setLemma(lemma);
            newLemma.setFrequency(1);
            siteLemmas.add(newLemma);
        }
        dbConnection.updateLemmasForSite(page.getSite(), siteLemmas);
        updateSearchIndex(pageLemmas, siteLemmas);
    }

    private void updateSearchIndex(HashMap<String, Integer> pageLemmas, List<SiteLemma> siteLemmas) {
        List<SearchIndex> newIndexes = new ArrayList<>();
        for (SiteLemma siteLemma : siteLemmas) {
            SearchIndex newIndex = new SearchIndex();
            newIndex.setPage(page);
            newIndex.setLemma(siteLemma);
            newIndex.setRank_value(pageLemmas.get(siteLemma.getLemma()));
            newIndexes.add(newIndex);
        }
        dbConnection.updateIndexes(page.getSite(), newIndexes);
    }

}