package searchengine.model.dbconnectors;

import org.springframework.stereotype.Component;
import searchengine.dto.search.SearchItem;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.SiteLemma;
import searchengine.services.search.LemmaData;
import searchengine.services.search.PageRelevance;
import searchengine.utils.WebUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Component
public class DbConnectorSearch extends DbConnectorBasic {

    public int countPages(List<IndexedSite> indexedSites) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT COUNT(*) AS `count_pages` FROM `page` WHERE (");
        for (IndexedSite site : indexedSites) {
            queryBuilder.append("`site_id` = '" + site.getId() + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length());
        queryBuilder.append(")");
        String query = queryBuilder.toString();
        return requestIntValue(query, "count_pages");
    }

    public List<LemmaData> requestLemmasFrequencies(
            List<IndexedSite> sites, Set<String> lemmas, int maxFrequency
    ) throws SQLException
    {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT `lemma`, SUM(`frequency`) AS `frequency` FROM `lemma` WHERE (");
        for (IndexedSite site : sites) {
            queryBuilder.append("`site_id` = '" + site.getId() + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") AND (");
        for (String lemma : lemmas) {
            queryBuilder.append("`lemma` = '" + lemma + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") ");
        queryBuilder.append("GROUP BY `lemma` ORDER BY `frequency`");
        String query = queryBuilder.toString();
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            List<LemmaData> lemmasFrequencies = new ArrayList<>();
            while (resultSet.next()) {
                if (resultSet.getInt("frequency") > maxFrequency) {
                    break;
                }
                LemmaData lemmaData = new LemmaData();
                lemmaData.setLemma(resultSet.getString("lemma"));
                lemmaData.setFrequency(resultSet.getInt("frequency"));
                lemmasFrequencies.add(lemmaData);
            }
            closeConnectionToDB();
            return lemmasFrequencies;
        }
    }

    public List<SiteLemma> requestLemmaIds(List<LemmaData> lemmasData) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT `lemma`,`id` FROM `lemma`WHERE (");
        for (LemmaData lemmaData : lemmasData) {
            queryBuilder.append("`lemma` = '" + lemmaData.getLemma() + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") ");
        queryBuilder.append("ORDER BY `lemma`");
        String query = queryBuilder.toString();
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            List<SiteLemma> orderedByLemma = new ArrayList<>();
            while (resultSet.next()) {
                SiteLemma siteLemma = new SiteLemma();
                siteLemma.setLemma(resultSet.getString("Lemma"));
                siteLemma.setId(resultSet.getInt("id"));
                orderedByLemma.add(siteLemma);
            }
            closeConnectionToDB();
            return orderedByLemma;
        }
    }

    public List<Integer> requestPagesIds(LemmaData lemmaData, List<Integer> pagesIds) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT DISTINCT `page_id` FROM `index`WHERE (");
        for (int lemmaId : lemmaData.getIds()) {
            queryBuilder.append("`lemma_id` = '" + lemmaId + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") ");
        if (!pagesIds.isEmpty()) {
            queryBuilder.append(" AND (");
            for (int pageId : pagesIds) {
                queryBuilder.append("`page_id` = '" + pageId + "' OR ");
            }
            queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") ");
        }
        String query = queryBuilder.toString();
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            List<Integer> requestedPagesIds = new ArrayList<>();
            while (resultSet.next()) {
                requestedPagesIds.add(resultSet.getInt("page_id"));
            }
            closeConnectionToDB();
            return requestedPagesIds;
        }
    }

    public List<PageRelevance> requestPagesRelevance(
            List<Integer> pagesIds, List<Integer> allLemmasIds, int limit, int offset
    ) throws SQLException
    {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT `page_id`, SUM(`rank`) AS `abs_rel`, COUNT(`lemma_id`) AS `lem_num` FROM `index` WHERE (");
        for (int pageId : pagesIds) {
            queryBuilder.append("`page_id` = '" + pageId + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") AND (");
        for (int lemmaId : allLemmasIds) {
            queryBuilder.append("`lemma_id` = '" + lemmaId + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") ");
        queryBuilder.append("GROUP BY `page_id` ORDER BY `abs_rel` DESC ");
        queryBuilder.append("LIMIT " + limit + " OFFSET " + offset);
        String query = queryBuilder.toString();
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            List<PageRelevance> pagesRelevance = new ArrayList<>();
            while (resultSet.next()) {
                PageRelevance pageRelevance = new PageRelevance();
                pageRelevance.setPageId(resultSet.getInt("page_id"));
                pageRelevance.setRelevance(resultSet.getFloat("abs_rel"));
                pagesRelevance.add(pageRelevance);
            }
            closeConnectionToDB();
            return pagesRelevance;
        }
    }

    public HashMap<Integer, SearchItem> requestPagesData(
            List<Integer> pagesIds, List<LemmaData> lemmasData) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(
            "SELECT " +
                "`site`.`url` AS `site_url`, " +
                "`site`.`name` AS `site_name`, " +
                "`page`.`path` AS `page_path`, " +
                "`page`.`content` AS `page_content`, " +
                "`page`.`id` AS `page_id` " +
            "FROM `page` " +
            "JOIN `site` ON `site`.`id` = `page`.`site_id` " +
            "WHERE ("
        );
        for (int pageId : pagesIds) {
            queryBuilder.append("`page`.`id` = '" + pageId + "' OR ");
        }
        queryBuilder.delete(queryBuilder.length() - 4, queryBuilder.length()).append(") ");
        String query = queryBuilder.toString();
        synchronized (this) {
            connectToDb();
            ResultSet resultSet = statement.executeQuery(query);
            HashMap<Integer, SearchItem> searchItemsMap = new HashMap<>();
            while (resultSet.next()) {
                int pageId = resultSet.getInt("page_id");
                SearchItem item = new SearchItem();
                searchItemsMap.put(pageId, item);
                item.setSite(resultSet.getString("site_url"));
                item.setSiteName(resultSet.getString("site_name"));
                item.setUri(resultSet.getString("page_path"));
                item.setTitle(WebUtils.findTitle(resultSet.getString("page_content")));
                item.setSnippet(WebUtils.makeSnippet(resultSet.getString("page_content"), lemmasData));
            }
            closeConnectionToDB();
            return searchItemsMap;
        }
    }

}