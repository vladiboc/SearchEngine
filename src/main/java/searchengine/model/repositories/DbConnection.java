package searchengine.model.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.DataSource;
import searchengine.config.Site;
import searchengine.config.SpringSettings;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class DbConnection {

    @Autowired
    private IndexedSiteRepository indexedSiteRepository;
    @Autowired
    private IndexedSitePageRepository indexedSitePageRepository;
    private final SpringSettings springSettings;
    private Connection connection;
    private Statement statement;

    public synchronized void saveSite(IndexedSite site) {
        indexedSiteRepository.save(site);
    }

    public synchronized void savePage(IndexedPage page) {
        indexedSitePageRepository.save(page);
    }

    public boolean clearSiteData(Site site) {
        boolean isCleared = false;
        String query = "DELETE FROM `site` WHERE `name` = '" + site.getName() + "'";
        try {
            connectToDb();
            statement.execute(query);
            closeConnectionToDB();
            isCleared = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isCleared;
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

}