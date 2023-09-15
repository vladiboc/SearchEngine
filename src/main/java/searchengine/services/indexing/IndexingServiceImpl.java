package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ApiResult;
import searchengine.model.dbconnectors.DbcIndexing;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.IndexingStatus;
import searchengine.utils.ConfigPlug;
import searchengine.utils.WebUtils;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService  {

    private final HashMap<Site, ForkJoinPool> collectingPools = new HashMap<>();
    private final DbcIndexing dbcIndexing;

    @Override
    public ApiResponse startIndexing() {
        if (hasCollectingThreads()) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация уже запущена"));
        }
        collectingPools.clear();
        for(Site site : ConfigPlug.getSitesList().getSites()) {
            final boolean isSiteDataCleared = dbcIndexing.clearSiteData(site);
            if (!isSiteDataCleared) {
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        new ApiError("Ошибка БД при удалении данных сайта " + site.getUrl())
                );
            }
            IndexedSite indexedSite = makeIndexedSite(site);
            if (indexedSite == null) {
                return new ApiResponse(HttpStatus.BAD_REQUEST,
                        new ApiError("Задан некорректный URL в конфиге " + site.getUrl()));
            }
            IndexedPage rootPage = makeRootPage(indexedSite);
            collectingPools.put(site, new ForkJoinPool());
            new Thread(
                () -> collectingPools.get(site).invoke(
                    new PageCollector(rootPage, site.getHttpRequestDelay(), dbcIndexing)
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
        final URL requestedUrl = WebUtils.makeUrlFromString(normalizeStringUrl(pageUrl));
        if (requestedUrl == null) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("Задана некорректная строка URL"));
        }
        final Site siteFromConfig = ConfigPlug.searchInConfiguration(requestedUrl);
        IndexedSite indexedSite = dbcIndexing.requestSiteFromDatabaseByUrl(WebUtils.makeStringFromUrl(requestedUrl));
        if (siteFromConfig == null && indexedSite == null) {
            return new ApiResponse(HttpStatus.BAD_REQUEST, new ApiError("Заданного сайта нет в конфигурации"));
        }
        if (indexedSite == null) {
            indexedSite = makeIndexedSite(siteFromConfig);
            if (indexedSite == null) {
                return new ApiResponse(HttpStatus.BAD_REQUEST,
                        new ApiError("Задан некорректный URL в конфиге " + siteFromConfig.getUrl()));
            }
        }
        IndexedPage page = makeIndexedPage(indexedSite, requestedUrl.getPath());
        final WebPage webPage = new WebPage(page);
        final Document jsoupDocument = webPage.requestWebPage();
        if (jsoupDocument == null) {
            updateSiteAndPage(page);
            return new ApiResponse(HttpStatus.OK, new ApiResult(true));
        }
        webPage.resetSiteUrl(jsoupDocument);
        tryToUpdateSiteFromDbWithNewUrl(page);
        if (!dbcIndexing.clearPageData(page)) {
            return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, new ApiError(
                "Ошибка при очистке данных страницы " + page.getSite().getUrl() + page.getPath()));
        }
        updateSiteAndPageNoErrors(page);
        LemmaIndexCollector lemmaIndexCollector = new LemmaIndexCollector(page, dbcIndexing);
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

   private @Nullable IndexedSite makeIndexedSite(Site siteFromConfig) {
       IndexedSite indexedSite = new IndexedSite();
       indexedSite.setIndexingStatus(IndexingStatus.INDEXING);
       indexedSite.setIndexingStatusTime(LocalDateTime.now());
       indexedSite.setLastError("");
       String siteFromConfigUrl = siteFromConfig.getUrl().replaceAll("\\/+$", "");
       if (WebUtils.makeUrlFromString(siteFromConfigUrl) == null) {
           return null;
       }
       indexedSite.setUrl(siteFromConfigUrl);
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

    private void tryToUpdateSiteFromDbWithNewUrl(IndexedPage page) {
        if (page.getSite().getId() == 0) {
            IndexedSite savedSite = dbcIndexing.requestSiteFromDatabaseByUrl(page.getSite().getUrl());
            if (savedSite != null) {
                page.setSite(savedSite);
            }
        }
    }

    private void updateSiteAndPage(IndexedPage page) {
        page.getSite().setIndexingStatus(IndexingStatus.INDEXED);
        dbcIndexing.updateSiteAndPage(page);
    }

    private void updateSiteAndPageNoErrors(IndexedPage page) {
        page.getSite().setLastError("");
        updateSiteAndPage(page);
    }
}