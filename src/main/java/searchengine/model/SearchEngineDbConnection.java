package searchengine.model;

import lombok.RequiredArgsConstructor;
import searchengine.config.DataSource;

import java.sql.Connection;
import java.sql.Statement;

@RequiredArgsConstructor
public class SearchEngineDbConnection {

    private final DataSource dataSource;

    private Connection connection;
    private Statement statement;

    public boolean clearTable(String siteName) {
        boolean tableIsCleared = false;
        return tableIsCleared;
    }

    private void open() {

        dataSource.getPassword();
    }

    private void close() {
    }

}
