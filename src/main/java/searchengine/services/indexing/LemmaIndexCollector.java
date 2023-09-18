package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import searchengine.model.dbconnectors.DbConnectorIndexing;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.SearchIndex;
import searchengine.model.entities.SiteLemma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class LemmaIndexCollector {
    private IndexedPage page;
    private DbConnectorIndexing dbcIndexing;

    public void collectAndUpdateLemmasAndIndex() {
        TextParser textParser = TextParser.makeTextParser();
        if (textParser == null) {
            return;
        }
        HashMap<String, Integer> pageLemmas = textParser.makeLemmas(page.getContent());
        updateSiteLemmasAndIndex(pageLemmas);
    }

    private void updateSiteLemmasAndIndex(HashMap<String, Integer> pageLemmas) {
        HashSet<String> pageLemmasStrings = new HashSet<>();
        pageLemmas.forEach((stringLemma, frequency) -> pageLemmasStrings.add(stringLemma));
        List<SiteLemma> storedPageLemmas = dbcIndexing.requestStoredPageLemmas(page.getSite(), pageLemmasStrings);
        List<SiteLemma> lemmasFromAnotherPage = new ArrayList<>();
        for (SiteLemma storedLemma : storedPageLemmas) {
            if (pageLemmasStrings.contains(storedLemma.getLemma())) {
                storedLemma.setFrequency(storedLemma.getFrequency() + 1);
                pageLemmasStrings.remove(storedLemma.getLemma());
            } else {
                lemmasFromAnotherPage.add(storedLemma);
            }
        }
        lemmasFromAnotherPage.forEach(lemma -> storedPageLemmas.remove(lemma));
        for (String pageLemmaString : pageLemmasStrings) {
            SiteLemma newLemma = new SiteLemma();
            newLemma.setSite(page.getSite());
            newLemma.setLemma(pageLemmaString);
            newLemma.setFrequency(1);
            storedPageLemmas.add(newLemma);
        }
        dbcIndexing.updateLemmasForSite(page.getSite(), storedPageLemmas);
        updateSearchIndex(pageLemmas, storedPageLemmas);
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
        dbcIndexing.updateIndexes(page.getSite(), newIndexes);
    }

}