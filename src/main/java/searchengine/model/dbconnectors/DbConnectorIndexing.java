package searchengine.model.dbconnectors;

import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.SearchIndex;
import searchengine.model.entities.SiteLemma;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class DbConnectorIndexing extends DbConnectorBasic {

    public boolean clearSiteData(Site site) {
        final String query = "DELETE FROM `site` WHERE `name` = '" + site.getName() + "'";
        return sqlQueryWithoutResultSet(query);
    }

    public boolean clearPageData(IndexedPage page) {
        boolean isCleared = true;
        if (page.getSite().getId() == 0) {
            return isCleared;
        }
        int pageId;
        try {
            pageId = requestPageIdFromDb(page);
        } catch (SQLException e) {
            e.printStackTrace();
            return !isCleared;
        }
        if (pageId == 0) {
            return isCleared;
        }
        List<Integer> pageLemmasIds;
        try {
            pageLemmasIds = requestLemmasListForPage(pageId);
        } catch (SQLException e) {
            e.printStackTrace();
            return !isCleared;
        }
        if (pageLemmasIds.isEmpty()) {
            return isCleared;
        }
        List<SiteLemma> lemmasOfPage = siteLemmaRepository.findAllById(pageLemmasIds);
        for (SiteLemma lemma : lemmasOfPage) {
            lemma.setFrequency(Math.min(0, lemma.getFrequency() - 1));
        }
        siteLemmaRepository.saveAll(lemmasOfPage);
        clearZeroLemmas();
        return removePageFromPagesTable(page);
    }

    public void updateSiteAndPage(IndexedPage page) {
        updateSite(page.getSite());
        savePage(page);
    }

    public synchronized void updateSite(IndexedSite site) {
        site.setIndexingStatusTime(LocalDateTime.now());
        saveSite(site);
    }

    public List<SiteLemma> requestStoredPageLemmas(IndexedSite site, Set<String> pageLemmaStrings) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM `lemma` WHERE `site_id` = '" + site.getId() + "'");
        if (!pageLemmaStrings.isEmpty()) {
            queryBuilder.append(" AND (");
            pageLemmaStrings.forEach(stringLemma -> queryBuilder.append("`lemma` = '" + stringLemma + "' OR "));
            queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length());
            queryBuilder.append(")");
        }
        String query = queryBuilder.toString();
        try {
            synchronized (this) {
                connectToDb();
                ResultSet resultSet = statement.executeQuery(query);
                List<SiteLemma> storedPageLemmas = new ArrayList<>();
                while (resultSet.next()) {
                    SiteLemma lemma = new SiteLemma();
                    lemma.setId(resultSet.getInt("id"));
                    lemma.setSite(site);
                    lemma.setLemma(resultSet.getString("lemma"));
                    lemma.setFrequency(resultSet.getInt("frequency"));
                    storedPageLemmas.add(lemma);
                }
                closeConnectionToDB();
                return storedPageLemmas;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void updateLemmasForSite(IndexedSite site, List<SiteLemma> lemmas) {
        updateSite(site);
        saveLemmas(lemmas);
    }

    public void updateIndexes(IndexedSite site, List<SearchIndex> newIndexes) {
        updateSite(site);
        saveIndexes(newIndexes);
    }

    private int requestPageIdFromDb(IndexedPage page) throws SQLException {
        String query = "SELECT * FROM `page` " +
                "WHERE `site_id` = '" + page.getSite().getId() + "' AND `path` = '" + page.getPath() + "'";
        return this.requestIntValue(query, "id");
    }

    private List<Integer> requestLemmasListForPage(int pageId) throws SQLException {
        String query = "SELECT `lemma_id` FROM `index` " + "WHERE `page_id` = '" + pageId + "'";
        List<Integer> pageLemmasIds = new ArrayList<>();
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                pageLemmasIds.add(resultSet.getInt("lemma_id"));
            }
            closeConnectionToDB();
        }
        return pageLemmasIds;
    }

    private boolean removePageFromPagesTable(IndexedPage page) {
        final String query = "DELETE FROM `page` " +
                "WHERE `site_id` = '" + page.getSite().getId() +
                "' AND `path` = '" + page.getPath() + "'";
        return sqlQueryWithoutResultSet(query);
    }

    private void clearZeroLemmas() {
        final String query = "DELETE FROM `lemma` " + "WHERE `frequency` = '0'";
        sqlQueryWithoutResultSet(query);
    }

    private synchronized void saveSite(IndexedSite site) {
        indexedSiteRepository.save(site);
    }

    private synchronized void savePage(IndexedPage page) {
        indexedSitePageRepository.save(page);
    }

    private synchronized void saveLemmas(List<SiteLemma> lemmas) {
        siteLemmaRepository.saveAll(lemmas);
    }

    private synchronized void saveIndexes(List<SearchIndex> indexes) {
        searchIndexRepository.saveAll(indexes);
    }
}