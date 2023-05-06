package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ApiResponseResult;
import searchengine.dto.anyservice.ApiError;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    static private boolean indexingIsRunning = false;

    private final SitesList sites;

    public ApiResponse startIndexing() {
        if (indexingIsRunning) {
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED,  new ApiError("Индексация уже запушена"));
        }
        return new ApiResponse(HttpStatus.OK, new ApiResponseResult(true));
    }

    public ApiResponse stopIndexing() {
        if (!indexingIsRunning) {
            List<Site> sitesFromConfig = sites.getSites();
            StringBuilder sitesString = new StringBuilder();
            for (Site site : sitesFromConfig) {
                sitesString.append(" ").append(site.getName());
            }
            String errorString = "Индексация не запущена" + sitesString.toString();
            return new ApiResponse(HttpStatus.METHOD_NOT_ALLOWED, new ApiError(errorString));
        }
        return new ApiResponse(HttpStatus.OK, new ApiResponseResult(true));
    }
}
