package searchengine.services.indexing;

import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionHeaders;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ApiResult;
import searchengine.model.entities.*;
import searchengine.model.DbConnection;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
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

    @Override
    public ApiResponse startIndexing() {
        if (hasCollectingThreads()) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация уже запущена"));
        }
        collectingPools.clear();
        for(Site site : sites.getSites()) {
            final boolean isSiteDataCleared = dbConnection.clearSiteData(site);
            if (!isSiteDataCleared) {
                ApiError apiError = new ApiError("Ошибка БД при удалении данных сайта " + site.getUrl());
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, apiError);
            }
            IndexedSite indexingSite = makeIndexedSite(site);
            IndexedPage rootPage = makeRootPage(indexingSite);
            collectingPools.put(site, new ForkJoinPool());
            new Thread(
                () -> collectingPools.get(site).invoke(
                    new PageCollector(rootPage, site.getHttpRequestDelay(), dbConnection)
                )
            ).start();
        }
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    @Override
    public ApiResponse stopIndexing() {
        if (!hasCollectingThreads()) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация не запущена"));
        }
        collectingPools.forEach((site, forkJoinPool) -> forkJoinPool.shutdownNow());
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    @Override
    public ApiResponse indexPage(final String pageUrl) {
        final URL requestedUrl = WebPage.makeUrlFromString(normalizeStringUrl(pageUrl));
        if (requestedUrl == null) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("Задана некорректная строка URL"));
        }
        final Site siteFromConfig = searchInConfiguration(requestedUrl);
        IndexedSite indexedSite = searchInSiteDatabase(WebPage.makeStringFromUrl(requestedUrl));
        if (siteFromConfig == null && indexedSite == null) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("Заданного сайта нет в конфигурации"));
        }
        if (indexedSite == null) {
            indexedSite = makeIndexedSite(siteFromConfig);
        }
        IndexedPage page = makeIndexedPage(indexedSite, requestedUrl.getPath());
        final WebPage webPage = new WebPage(page, requestedUrl);
        final Document jsoupDocument = webPage.requestWebPage();
        if (jsoupDocument == null) {
            updateSiteAndPage(page);
            return new ApiResponse(HttpStatus.OK, new ApiResult(true));
        }
        webPage.resetSiteUrl(jsoupDocument);
        searchForSiteRecordAgainAfterResettingSiteUrl(page);
        boolean isClearedPageData = dbConnection.clearPageData(page);
        if (!isClearedPageData) {
            return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, new ApiError(
                "Ошибка при очистке данных страницы " + page.getSite().getUrl() + page.getPath()
            ));
        }
        page.getSite().setLastError("");
        updateSiteAndPage(page);
        LemmaIndexCollector lemmaIndexCollector = new LemmaIndexCollector(page, dbConnection);
        lemmaIndexCollector.collectAndUpdateLemmasAndIndex();
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

   private IndexedSite makeIndexedSite(Site siteFromConfig) {
       IndexedSite indexedSite = new IndexedSite();
       indexedSite.setIndexingStatus(IndexingStatus.INDEXING);
       indexedSite.setIndexingStatusTime(LocalDateTime.now());
       indexedSite.setLastError("");
       indexedSite.setUrl(siteFromConfig.getUrl().replaceAll("\\/+$", ""));
       indexedSite.setName(siteFromConfig.getName());
       return indexedSite;
   }

    private IndexedPage makeRootPage(final IndexedSite indexedSite) {
        return makeIndexedPage(indexedSite, "/");
    }

    private IndexedPage makeIndexedPage(final IndexedSite indexedSite, final String path) {
        IndexedPage indexedPage = new IndexedPage();
        indexedPage.setSite(indexedSite);
        indexedPage.setPath(path);
        indexedPage.setHttpResponseCode(0);
        indexedPage.setContent("");
        return indexedPage;
    }

    private String normalizeStringUrl(final String pageUrl) {
        return URLDecoder.decode(pageUrl.replaceAll("^url=", ""), StandardCharsets.UTF_8);
    }

    private @Nullable Site searchInConfiguration(URL requestedUrl) {
        for (Site site : sites.getSites()) {
            URL specifiedUrl = WebPage.makeUrlFromString(site.getUrl());
            if (specifiedUrl == null) {
                continue;
            }
            if (requestedUrl.getHost().equals(specifiedUrl.getHost())) {
                return site;
            }
        }
        return null;
    }

    private @Nullable IndexedSite searchInSiteDatabase(String neededSiteUrl) {
        List<IndexedSite> indexedSites = dbConnection.getIndexedSites();
        for (IndexedSite indexedSite : indexedSites) {
            if (neededSiteUrl.equals(indexedSite.getUrl())) {
                return indexedSite;
            }
        }
        return null;
    }

    private void searchForSiteRecordAgainAfterResettingSiteUrl(IndexedPage page) {
        if (page.getSite().getId() == 0) {
            IndexedSite savedSite = searchInSiteDatabase(page.getSite().getUrl());
            if (savedSite != null) {
                page.setSite(savedSite);
            }
        }
    }

    private void updateSiteAndPage(IndexedPage page) {
        page.getSite().setIndexingStatus(IndexingStatus.INDEXED);
        dbConnection.updateSiteAndPage(page);
    }

}