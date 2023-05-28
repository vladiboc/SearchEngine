package searchengine.model.dbconnection;

import lombok.RequiredArgsConstructor;
import searchengine.config.DataSource;
import searchengine.config.Site;

import java.sql.*;

@RequiredArgsConstructor
public class DbConnection {

    private final DataSource dataSource;

    private Connection connection;
    private Statement statement;

    public void clearSiteData(Site site) throws SQLException {
        connectToDb();
        String query = "DELETE FROM `site` WHERE `url` = '" + site.getUrl() + "'";
        statement.execute(query);
        closeConnectionToDB();
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
