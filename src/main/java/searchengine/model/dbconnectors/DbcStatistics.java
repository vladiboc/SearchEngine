package searchengine.model.dbconnectors;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.entities.IndexedSite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
@RequiredArgsConstructor
public class DbcStatistics extends DbcBasic {

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

    public @Nullable DetailedStatisticsItem requestDetailedSiteStatistics(IndexedSite indexedSite) {
        DetailedStatisticsItem siteStatistics = new DetailedStatisticsItem();
        siteStatistics.setUrl(indexedSite.getUrl());
        siteStatistics.setName(indexedSite.getName());
        siteStatistics.setStatus(indexedSite.getIndexingStatus().toString());
        Timestamp timestamp = Timestamp.valueOf(indexedSite.getIndexingStatusTime());
        siteStatistics.setStatusTime(timestamp.getTime());
        siteStatistics.setError(indexedSite.getLastError());
        try {
            siteStatistics.setPages(this.requestIntValue(
            "SELECT COUNT(*) AS `pages_amount` FROM `page` WHERE `site_id` = '" + indexedSite.getId() + "'",
            "pages_amount"
            ));
            siteStatistics.setLemmas(this.requestIntValue(
            "SELECT COUNT(*) AS `pages_amount` FROM `lemma` WHERE `site_id` = '" + indexedSite.getId() + "'",
            "pages_amount"
            ));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return siteStatistics;
    }

}