package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ApiResult;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping({"/statistics", "/startIndexing", "/stopIndexing"})
    public ResponseEntity<ApiResult> getUrlWithoutParameters(HttpServletRequest request) {
        ApiResponse response = switch (request.getRequestURI()) {
            case "/api/statistics"    -> statisticsService.getStatistics();
            case "/api/startIndexing" -> indexingService.startIndexing();
            case "/api/stopIndexing"  -> indexingService.stopIndexing();
            default -> new ApiResponse(HttpStatus.NOT_FOUND, new ApiError("Нет метода для этого URI"));
        };
        return new ResponseEntity<>(response.getResult(), response.getStatus());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ApiResult> postIndexOnePage(@RequestBody String pageUrl) {
        ApiResponse response = indexingService.indexPage(pageUrl);
        return new ResponseEntity<>(response.getResult(), response.getStatus());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResult> getParametersAndSearch(HttpServletRequest request) {
        String site = request.getParameter("site");
        String query = request.getParameter("query");
        String offset = request.getParameter("offset");
        String limit = request.getParameter("limit");
        ApiResponse response = searchService.search(site, query, offset, limit);
        return new ResponseEntity<>(response.getResult(), response.getStatus());
    }

}