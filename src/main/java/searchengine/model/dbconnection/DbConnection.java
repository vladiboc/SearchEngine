package searchengine.model.dbconnection;

import org.springframework.stereotype.Component;
import searchengine.config.DataSource;
import searchengine.config.Site;
import searchengine.config.SpringSettings;
import searchengine.model.entities.IndexedSite;

import java.sql.*;
import java.util.HashSet;

@Component
public class DbConnection {

    private static SpringSettings springSettings;
    private final DataSource dataSource;
    private Connection connection;
    private Statement statement;

    public DbConnection() {
        this.dataSource = springSettings.getDatasource();
    }

    public boolean clearSiteData(Site site) {
        boolean isCleared = false;
        String query = "DELETE FROM `site` WHERE `url` = '" + site.getUrl() + "'";
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

    public HashSet<String> getCollectedPaths(IndexedSite site) {
        HashSet<String> collectedPaths = new HashSet<String>();
        String query = "SELECT `path` FROM `page` WHERE `site_id`=" + site.getId() + ";";
        try {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                collectedPaths.add(resultSet.getString("path"));
            }
            resultSet.close();
            closeConnectionToDB();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return collectedPaths;
    }

    private void connectToDb() throws SQLException {
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
