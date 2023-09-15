package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.search.SearchItem;
import searchengine.dto.search.SearchResult;
import searchengine.model.dbconnectors.DbcSearch;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.SiteLemma;
import searchengine.services.indexing.TextParser;
import searchengine.utils.ConfigPlug;
import searchengine.utils.WebUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final DbcSearch dbcSearch;
    private Set<String> queryLemmas;

    @Override
    public ApiResponse search( @Nullable String requestedSite, @Nullable String query,
            @Nullable String offset, @Nullable String limit) {
        int intOffset = (offset == null || Integer.parseInt(offset) < 0) ? 0 : Integer.parseInt(offset);
        int intLimit = (limit == null || Integer.parseInt(limit) <= 0) ? 20 : Integer.parseInt(limit);
        if (query == null) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("Не задан поисковый запрос"));
        }
        final IndexedSite indexedSite = searchIndexedSite(requestedSite);
        if (requestedSite != null && indexedSite == null) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("Сайт " + requestedSite + "не индексирован"));
        }
        final TextParser textParser = TextParser.makeTextParser();
        if (textParser == null) {
            return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, new ApiError("Не удалось создать парсер лемм"));
        }
        this.queryLemmas = textParser.makeLemmas(query).keySet();
        if (queryLemmas.isEmpty()) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("Поисковый запрос не содержит лемм"));
        }
        List<IndexedSite> indexedSites = new ArrayList<>();
        if (indexedSite != null) {
            indexedSites.add(indexedSite);
        } else {
            indexedSites = dbcSearch.requestSuccessfullyIndexedSites();
        }
        if (indexedSites.isEmpty()) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("В базе нет проиндексированных сайтов"));
        }
        final SearchResult searchResult;
        try {
            searchResult = doSearch(indexedSites, intOffset, intLimit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, new ApiError("Ошибка запроса к базе данных"));
        }
        return new ApiResponse(HttpStatus.OK, searchResult);
    }

    private @Nullable IndexedSite searchIndexedSite (String requestedSite) {
        final URL requestedUrl = WebUtils.makeUrlFromString(requestedSite);
        if (requestedUrl == null) {
            return null;
        }
        final Site siteFromConfig = ConfigPlug.searchInConfiguration(requestedUrl);
        if (siteFromConfig == null) {
            return dbcSearch.requestSiteFromDatabaseByUrl(WebUtils.makeStringFromUrl(requestedUrl));
        } else {
            return dbcSearch.requestSiteFromDatabaseByName(siteFromConfig.getName());
        }
    }

    private SearchResult doSearch(List<IndexedSite> indexedSites, int intOffset, int intLimit) throws SQLException {
        int pagesCount = dbcSearch.countPages(indexedSites);
        int maxFrequency = pagesCount / 2 + 1;
        List<LemmaData> orderedLemmasData = dbcSearch.requestLemmasFrequencies(indexedSites, this.queryLemmas, maxFrequency);
        fillLemmasIds(orderedLemmasData);
        List<Integer> pagesIds = findPagesIds(orderedLemmasData);
        final SearchResult searchResult = new SearchResult();
        if (pagesIds.isEmpty()) {
            return searchResult;
        }
        searchResult.setCount(pagesIds.size());
        searchResult.setData(fillSearchItems(pagesIds, orderedLemmasData, intOffset, intLimit));
        return searchResult;
    }

    private List<LemmaData> fillLemmasIds(List<LemmaData> lemmasData) throws SQLException {
        TreeMap<String, LemmaData> lemmasDataMap = new TreeMap<>();
        lemmasData.forEach(e -> lemmasDataMap.put(e.getLemma(), e));
        List<SiteLemma> orderedByLemmaIds = dbcSearch.requestLemmaIds(lemmasData);
        int i = 0;
        for (String lemma : lemmasDataMap.keySet()) {
            while (i < orderedByLemmaIds.size() && orderedByLemmaIds.get(i).getLemma().equals(lemma)) {
                lemmasDataMap.get(lemma).getIds().add(orderedByLemmaIds.get(i++).getId());
            }
        }
        return lemmasData;
    }

    private List<Integer> findPagesIds(List<LemmaData> lemmasData) throws SQLException {
        List<Integer> pagesIds = new ArrayList<>();
        for (LemmaData lemmaData : lemmasData) {
            pagesIds = dbcSearch.requestPagesIds(lemmaData, pagesIds);
            if (pagesIds.isEmpty()) {
                break;
            }
        }
        return pagesIds;
    }

    private List<SearchItem> fillSearchItems(
            List<Integer> pagesIds, List<LemmaData> orderedLemmasData, int offset, int limit
    ) throws SQLException
    {
        List<Integer> allLemmasIds = new ArrayList<>();
        orderedLemmasData.forEach(lemmaData -> allLemmasIds.addAll(lemmaData.getIds()));
        List<PageRelevance> pageRelevance = dbcSearch.requestPagesRelevance(pagesIds, allLemmasIds, limit, offset);
        HashMap<Integer, SearchItem> pagesData = dbcSearch.requestPagesData(pagesIds, orderedLemmasData);
        List<SearchItem> searchItems = new ArrayList<>();
        for (PageRelevance relevance : pageRelevance) {
            SearchItem searchItem = pagesData.get(relevance.getPageId());
            searchItem.setRelevance(relevance.getRelevance());
            searchItems.add(searchItem);
        }
        return searchItems;
    }

}