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
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingServiceImpl implements IndexingService {

    private static ConnectionHeaders headersFromConfig;
    private final HashMap<Site, ForkJoinPool> collectingPools;
    private final DbConnection dbConnection;
    private final SitesList sites;

    public static ConnectionHeaders getConnectionHeaders() {
        return headersFromConfig;
    }

    public IndexingServiceImpl(SitesList sites, DbConnection dbConnection, ConnectionHeaders connectionHeaders) {
        this.collectingPools = new HashMap<>();
        this.dbConnection = dbConnection;
        this.sites = sites;
        headersFromConfig = connectionHeaders;
    }

    public ApiResponse startIndexing() {
        if (hasCollectingThreads()) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация уже запущена"));
        }
        collectingPools.clear();
        for(Site site : sites.getSites()) {
            boolean isSiteDataCleared = dbConnection.clearSiteData(site);
            if (!isSiteDataCleared) {
                ApiError apiError = new ApiError("Ошибка БД при удалении данных сайта " + site.getUrl());
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, apiError);
            }
            IndexedPage rootPage = saveIndexedSiteToDb(site);
            collectingPools.put(site, new ForkJoinPool());
            new Thread(
                () -> collectingPools.get(site).invoke(
                    new PageCollector(rootPage, site.getHttpRequestDelay(), dbConnection)
                )
            ).start();
        }
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    public ApiResponse stopIndexing() {
        if (!hasCollectingThreads()) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация не запущена"));
        }
        collectingPools.forEach((site, forkJoinPool) -> {
            forkJoinPool.shutdownNow();
        });
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    private boolean hasCollectingThreads() {
        boolean hasActive = false;
        synchronized (collectingPools) {
            for (Site site : collectingPools.keySet()) {
                hasActive = hasActive
                    || collectingPools.get(site).hasQueuedSubmissions()
                    || collectingPools.get(site).getQueuedTaskCount() > 0
                    || collectingPools.get(site).getActiveThreadCount() > 0;
            }
        }
        return hasActive;
    }

    private IndexedPage saveIndexedSiteToDb(Site siteFromConfig) {
        IndexedSite indexedSite = new IndexedSite();
            indexedSite.setIndexingStatus(IndexingStatus.INDEXING);
            indexedSite.setIndexingStatusTime(LocalDateTime.now());
            indexedSite.setLastError("");
            indexedSite.setUrl(siteFromConfig.getUrl().replaceAll("\\/+$", ""));
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