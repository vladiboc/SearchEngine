package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SpringSettings;
import searchengine.config.DataSource;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ApiResult;
import searchengine.dto.anyservice.ApiError;
import searchengine.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    static private boolean indexingIsRunning = false;

    private final SitesList sites;
    private final SpringSettings springSettings;
    @Autowired
    private IndexedSiteRepository indexedSiteRepository;
    @Autowired
    private IndexedSitePageRepository indexedSitePageRepository;

    public ApiResponse startIndexing() {
        if (indexingIsRunning) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED,  new ApiError("Индексация уже запущена"));
        }

        indexingIsRunning = true;

        List<Site> sitesList = sites.getSites();
        ArrayList<Site> sitesFromConfig = new ArrayList<>();
        for (Site site : sitesList) {
            sitesFromConfig.add(site);
        }

        if (!indexingSite(sitesFromConfig.get(0), springSettings.getDatasource())) {
            String errorString = "Ошибки сервера при индексировании сайта " + sitesFromConfig.get(0).getName();
            return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR,  new ApiError(errorString));
        }

//        IndexedSite someSite = new IndexedSite();
//        someSite.setIndexingStatus(SiteIndexingStatus.INDEXING);
//        someSite.setIndexingStatusTime(LocalDateTime.now());
//        someSite.setLastError("");
//        someSite.setUrl(sitesFromConfig.get(0).getUrl());
//        someSite.setName(sitesFromConfig.get(0).getName());
//        indexedSiteRepository.save(someSite);

        indexingIsRunning = false;

        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }

    private boolean indexingSite(Site site, DataSource dataSource) {
        SearchEngineDbConnection dbConnection = new SearchEngineDbConnection(dataSource);
        boolean noInternalProblems = dbConnection.clearTable(site.getName());
        if (!noInternalProblems) {
            return false;
        }

        return noInternalProblems;
    }

    public ApiResponse stopIndexing() {
        if (!indexingIsRunning) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError("Индексация не запущена"));
        }
        return new ApiResponse(HttpStatus.OK, new ApiResult(true));
    }
}
