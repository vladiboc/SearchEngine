package searchengine.model;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.config.DataSource;
import searchengine.config.Site;
import searchengine.config.SpringSettings;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.SearchIndex;
import searchengine.model.entities.SiteLemma;
import searchengine.model.repositories.IndexedSitePageRepository;
import searchengine.model.repositories.IndexedSiteRepository;
import searchengine.model.repositories.SearchIndexRepository;
import searchengine.model.repositories.SiteLemmaRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DbConnection {
    @Autowired
    private IndexedSiteRepository indexedSiteRepository;
    @Autowired
    private IndexedSitePageRepository indexedSitePageRepository;
    @Autowired
    private SiteLemmaRepository siteLemmaRepository;
    @Autowired
    private SearchIndexRepository searchIndexRepository;
    private final SpringSettings springSettings;
    private Connection connection;
    private Statement statement;

    public boolean clearSiteData(Site site) {
        final String query = "DELETE FROM `site` WHERE `name` = '" + site.getName() + "'";
        return sqlQueryWithoutResultSet(query);
    }

    public boolean clearPageData(IndexedPage page) {
        boolean isCleared = true;
        if (page.getSite().getId() == 0) {
            return isCleared;
        }
        int pageId = 0;
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

    public void savePageAndSite(IndexedPage page) {
        updateSite(page.getSite());
        savePage(page);
    }

    public void updateSite(IndexedSite site) {
        synchronized (site) {
            site.setIndexingStatusTime(LocalDateTime.now());
        }
        saveSite(site);
    }

    public List<IndexedSite> getIndexedSites() {
        return indexedSiteRepository.findAll();
    }

    public List<SiteLemma> getAllLemmasForSite(IndexedSite site) {
        String query = "SELECT * FROM `lemma` WHERE `site_id` = '" + site.getId() + "'";
        try {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            List<SiteLemma> siteLemmas = new ArrayList<>();
            while (resultSet.next()) {
                SiteLemma lemma = new SiteLemma();
                lemma.setId(resultSet.getInt("id"));
                lemma.setSite(site);
                lemma.setLemma(resultSet.getString("lemma"));
                lemma.setFrequency(resultSet.getInt("frequency"));
                siteLemmas.add(lemma);
            }
            closeConnectionToDB();
            return siteLemmas;
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

    private synchronized boolean sqlQueryWithoutResultSet(String query) {
        try {
            connectToDb();
            statement.execute(query);
            closeConnectionToDB();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void connectToDb() throws SQLException {
        DataSource dataSource = springSettings.getDatasource();
        connection = DriverManager.getConnection(
            dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword()
        );
        statement = connection.createStatement();
    }

    private void closeConnectionToDB() throws SQLException {
        statement.close();
        connection.close();
    }

    private int requestPageIdFromDb(IndexedPage page) throws SQLException {
        String query = "SELECT * FROM `page` " +
                "WHERE `site_id` = '" + page.getSite().getId() + "' AND `path` = '" + page.getPath() + "'";
        connectToDb();
        ResultSet resultSet = statement.executeQuery(query);
        int pageId = 0;
        if (resultSet.next()) {
            pageId = resultSet.getInt("id");
        }
        closeConnectionToDB();
        return pageId;
    }

    private List<Integer> requestLemmasListForPage(int pageId) throws SQLException {
        String query = "SELECT `lemma_id` FROM `index` " + "WHERE `page_id` = '" + pageId + "'";
        List<Integer> pageLemmasIds = new ArrayList<>();
        connectToDb();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            pageLemmasIds.add(resultSet.getInt("lemma_id"));
        }
        closeConnectionToDB();
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