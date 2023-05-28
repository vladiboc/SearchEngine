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
import searchengine.model.repositories.IndexedSitePageRepository;
import searchengine.model.repositories.IndexedSiteRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private static volatile boolean isIndexingRunning = false;

    private final SitesList sites;
    private final SpringSettings springSettings;
    @Autowired
    private IndexedSiteRepository indexedSiteRepository;
    @Autowired
    private IndexedSitePageRepository indexedSitePageRepository;

    public ApiResponse startIndexing() {

        if (isIndexingRunning) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED,  new ApiError("Индексация уже запущена"));
        }
        isIndexingRunning = true;

        List<Site> sitesFromConfig = sites.getSites();

//        ExecutorService executorService = new RecursiveTask<>();
//        Executors

        for(Site site : sitesFromConfig) {
            try {
                DbConnection dbConnection = new DbConnection(springSettings.getDatasource());
                dbConnection.clearSiteData(site);
                indexingSite(site);
            } catch (SQLException ex) {
                ex.printStackTrace();
                ApiError apiError = new ApiError("Ошибка БД при удалении данных сайта " + site.getUrl());
                isIndexingRunning = false;
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, apiError);
            }
        }

//        sitesFromConfig.forEach(site -> {
//            IndexedSite someSite = new IndexedSite();
//            someSite.setIndexingStatus(SiteIndexingStatus.INDEXING);
//            someSite.setIndexingStatusTime(LocalDateTime.now());
//            someSite.setLastError("");
//            someSite.setUrl(site.getUrl());
//            someSite.setName(site.getName());
//            indexedSiteRepository.save(someSite);
//        });

        isIndexingRunning = false;
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    public ApiResponse stopIndexing() {
        if (!isIndexingRunning) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация не запущена"));
        }

        isIndexingRunning = false;
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    private void indexingSite(Site site) {

    }

}
