package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.SpringSettings;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ApiResult;
import searchengine.model.dbconnection.DbConnection;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.IndexingStatus;
import searchengine.model.repositories.IndexedSiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    @Autowired
    private IndexedSiteRepository indexedSiteRepository;
    private final SitesList sites;

    public ApiResponse startIndexing() {
        if (PageCollector.getCollectingThreadsNumber() > 0) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация уже запущена"));
        }
        List<Site> sitesFromConfig = sites.getSites();
        for(Site site : sitesFromConfig) {
            boolean isSiteDataCleared = (new DbConnection()).clearSiteData(site);
            if (!isSiteDataCleared) {
                ApiError apiError = new ApiError("Ошибка БД при удалении данных сайта " + site.getUrl());
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, apiError);
            }
            IndexedPage rootPage = saveIndexedSite(site);
            new Thread(() ->
                new ForkJoinPool().invoke(new PageCollector(rootPage, site.getHttpRequestDelay()))
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
        indexedSiteRepository.save(indexedSite);
        IndexedPage rootPage = new IndexedPage();
            rootPage.setSite(indexedSite);
            rootPage.setPath("/");
            rootPage.setHttpResponseCode(0);
            rootPage.setContent("");
        return rootPage;
    }

}