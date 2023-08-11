package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.statistics.StatisticsService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    @GetMapping({"/statistics", "/startIndexing", "/stopIndexing"})
    public ResponseEntity getUrlWithoutParameters(HttpServletRequest request) {
        ApiResponse response = switch (request.getRequestURI()) {
            case "/api/statistics"    -> statisticsService.getStatistics();
            case "/api/startIndexing" -> indexingService.startIndexing();
            case "/api/stopIndexing"  -> indexingService.stopIndexing();
            default -> new ApiResponse(HttpStatus.NOT_FOUND, new ApiError("Нет метода для этого URI"));
        };
        return new ResponseEntity(response.getResult(), response.getStatus());
    }

    @PostMapping("/indexPage")
    public ResponseEntity postIndexOnePage(@RequestBody String pageUrl) {
        ApiResponse response = indexingService.indexPage(pageUrl);
        return new ResponseEntity(response.getResult(), response.getStatus());
    }

}
