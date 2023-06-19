package searchengine.services.indexing;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionHeaders;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ApiResult;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.IndexingStatus;
import searchengine.model.repositories.DbConnection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingServiceImpl implements IndexingService {

    private static ConnectionHeaders headersFromConfig;
    private final SitesList sites;
    private final DbConnection dbConnection;
    private final ConnectionHeaders connectionHeaders;

    public static ConnectionHeaders getConnectionHeaders() {
        return headersFromConfig;
    }

    public IndexingServiceImpl(SitesList sites, DbConnection dbConnection, ConnectionHeaders connectionHeaders) {
        this.sites = sites;
        this.dbConnection = dbConnection;
        this.connectionHeaders = connectionHeaders;
        headersFromConfig = connectionHeaders;
    }

    public ApiResponse startIndexing() {
        if (PageCollector.getCollectingThreadsNumber() > 0) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация уже запущена"));
        }
        List<Site> sitesFromConfig = sites.getSites();
        for(Site site : sitesFromConfig) {
            boolean isSiteDataCleared = dbConnection.clearSiteData(site);
            if (!isSiteDataCleared) {
                ApiError apiError = new ApiError("Ошибка БД при удалении данных сайта " + site.getUrl());
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, apiError);
            }
            IndexedPage rootPage = saveIndexedSite(site);
            new Thread(() ->
                new ForkJoinPool().invoke(new PageCollector(rootPage, site.getHttpRequestDelay(), dbConnection))
            ).start();
        }
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    public ApiResponse stopIndexing() {
        if (PageCollector.getCollectingThreadsNumber() == 0) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация не запущена"));
        }
        PageCollector.interruptAlliCollectingThreads();
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    private IndexedPage saveIndexedSite(Site siteFromConfig) {
        IndexedSite indexedSite = new IndexedSite();
            indexedSite.setIndexingStatus(IndexingStatus.INDEXING);
            indexedSite.setIndexingStatusTime(LocalDateTime.now());
            indexedSite.setLastError("");
            indexedSite.setUrl(siteFromConfig.getUrl());
            indexedSite.setName(siteFromConfig.getName());
        dbConnection.saveSite(indexedSite);
        IndexedPage rootPage = new IndexedPage();
            rootPage.setSite(indexedSite);
            rootPage.setPath("/");
            rootPage.setHttpResponseCode(0);
            rootPage.setContent("");
        return rootPage;
    }

}