package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    @GetMapping({"/statistics", "/startIndexing", "/stopIndexing"})
    public ResponseEntity uriWithoutParameters(HttpServletRequest request) {
        ApiResponse response = getMethodByURI(request.getRequestURI());
        return new ResponseEntity(response.getResult(), response.getStatus());
    }

    private ApiResponse getMethodByURI(String uri) {
       return switch (uri) {
           case "/api/statistics"    -> statisticsService.getStatistics();
           case "/api/startIndexing" -> indexingService.startIndexing();
           case "/api/stopIndexing"  -> indexingService.stopIndexing();
           default -> new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, new ApiError("Нет метода для этого URI"));
       };
    }

}
