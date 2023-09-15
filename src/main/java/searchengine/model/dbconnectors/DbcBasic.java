package searchengine.model.dbconnectors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.config.DataSource;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.IndexingStatus;
import searchengine.model.repositories.IndexedSitePageRepository;
import searchengine.model.repositories.IndexedSiteRepository;
import searchengine.model.repositories.SearchIndexRepository;
import searchengine.model.repositories.SiteLemmaRepository;
import searchengine.utils.ConfigPlug;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DbcBasic {
    @Autowired
    protected IndexedSiteRepository indexedSiteRepository;
    @Autowired
    protected IndexedSitePageRepository indexedSitePageRepository;
    @Autowired
    protected SiteLemmaRepository siteLemmaRepository;
    @Autowired
    protected SearchIndexRepository searchIndexRepository;
    protected Connection connection;
    protected Statement statement;

    protected void connectToDb() throws SQLException {
        DataSource dataSource = ConfigPlug.getSpringSettings().getDatasource();
        connection = DriverManager.getConnection(
                dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword()
        );
        statement = connection.createStatement();
    }

    protected void closeConnectionToDB() throws SQLException {
        statement.close();
        connection.close();
    }

    protected synchronized boolean sqlQueryWithoutResultSet(String query) {
        try {
            synchronized (this) {
                connectToDb();
                statement.execute(query);
                closeConnectionToDB();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected int requestIntValue(String query, String columnLabel) throws SQLException {
        return (int) this.requestLongValue(query, columnLabel);
    }

    protected long requestLongValue(String query, String columnLabel) throws SQLException {
        long requestedLongValue = 0L;
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                requestedLongValue = resultSet.getLong(columnLabel);
            }
            closeConnectionToDB();
        }
        return requestedLongValue;
    }

    public @Nullable IndexedSite requestSiteFromDatabaseByUrl(String neededSiteUrl) {
        List<IndexedSite> indexedSites = requestIndexedSites();
        for (IndexedSite indexedSite : indexedSites) {
            if (neededSiteUrl.equals(indexedSite.getUrl())) {
                return indexedSite;
            }
        }
        return null;
    }

    public @Nullable IndexedSite requestSiteFromDatabaseByName(String siteName) {
        List<IndexedSite> indexedSites = requestIndexedSites();
        for (IndexedSite indexedSite : indexedSites) {
            if (siteName.equals(indexedSite.getName())) {
                return indexedSite;
            }
        }
        return null;
    }

    public List<IndexedSite> requestIndexedSites() {
        return indexedSiteRepository.findAll();
    }

    public List<IndexedSite> requestSuccessfullyIndexedSites() {
        List<IndexedSite> indexedSites = requestIndexedSites();
        List<IndexedSite> reallyIndexedSites = new ArrayList<>();
        indexedSites.forEach(site -> {
            if (site.getIndexingStatus().equals(IndexingStatus.INDEXED)) {
                reallyIndexedSites.add(site);
            }
        });
        return reallyIndexedSites;
    }

}