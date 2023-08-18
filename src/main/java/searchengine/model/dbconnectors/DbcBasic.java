package searchengine.model.dbconnectors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.DataSource;
import searchengine.config.SpringSettings;
import searchengine.model.repositories.IndexedSitePageRepository;
import searchengine.model.repositories.IndexedSiteRepository;
import searchengine.model.repositories.SearchIndexRepository;
import searchengine.model.repositories.SiteLemmaRepository;

import java.sql.*;

@Component
@RequiredArgsConstructor
public class DbcBasic {
    protected final SpringSettings springSettings;
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
        DataSource dataSource = springSettings.getDatasource();
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

    protected String requestStringValue(String query, String columnLabel) throws SQLException {
        String requestedStringValue = "";
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                requestedStringValue = resultSet.getString(columnLabel);
            }
            closeConnectionToDB();
        }
        return requestedStringValue;
    }
}
