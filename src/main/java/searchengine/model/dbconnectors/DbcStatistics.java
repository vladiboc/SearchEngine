package searchengine.model.dbconnectors;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.config.SpringSettings;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.TotalStatistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
public class DbcStatistics extends DbcBasic {

    DbcStatistics(SpringSettings springSettings) { super(springSettings);
    }

    public @Nullable TotalStatistics requestTotalStatistics() {
        TotalStatistics totalStatistics = new TotalStatistics();
        try {
            totalStatistics.setSites(this.requestIntValue(
                "SELECT COUNT(*) AS `sites_amount` FROM `site`", "sites_amount"));
            totalStatistics.setPages(this.requestIntValue(
                "SELECT COUNT(*) AS `pages_amount` FROM `page`", "pages_amount"));
            totalStatistics.setLemmas(this.requestIntValue(
                "SELECT COUNT(*) AS `lemmas_amount` FROM `lemma`", "lemmas_amount"));
            totalStatistics.setIndexing(this.requestTotalStatus());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return totalStatistics;
    }

    private boolean requestTotalStatus() {
        boolean isIndexing = false;
        String queryStatus = "SELECT `status` FROM `site`";
        try {
            synchronized (this) {
                connectToDb();
                ResultSet resultSet = statement.executeQuery(queryStatus);
                while (resultSet.next()) {
                    if (resultSet.getString("status").equals("INDEXING")) {
                        isIndexing = true;
                        break;
                    }
                }
                closeConnectionToDB();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isIndexing;
    }

    public @Nullable DetailedStatisticsItem requestSiteStatistics(String siteName) {
        DetailedStatisticsItem siteStatistics = new DetailedStatisticsItem();
        String querySite = "SELECT * FROM `site` WHERE `name` = '" + siteName + "'";
        try {
            int siteId = 0;
            synchronized (this) {
                connectToDb();
                ResultSet resultSet = statement.executeQuery(querySite);
                if (resultSet.next()) {
                    siteId = resultSet.getInt("id");
                    siteStatistics.setUrl(resultSet.getString("url"));
                    siteStatistics.setName(resultSet.getString("name"));
                    siteStatistics.setStatus(resultSet.getString("status"));
                    Timestamp timestamp = resultSet.getTimestamp("status_time");
                    siteStatistics.setStatusTime(timestamp.getTime());
                    siteStatistics.setError(resultSet.getString("last_error"));
                }
                closeConnectionToDB();
            }
            siteStatistics.setPages(this.requestIntValue(
                    "SELECT COUNT(*) AS `pages_amount` FROM `page` WHERE `site_id` = '" + siteId + "'",
                    "pages_amount"));
            siteStatistics.setLemmas(this.requestIntValue(
                    "SELECT COUNT(*) AS `pages_amount` FROM `lemma` WHERE `site_id` = '" + siteId + "'",
                    "pages_amount"));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return siteStatistics;
    }

}