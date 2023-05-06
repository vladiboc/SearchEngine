package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping("/statistics")
    public ResponseEntity statistics() {
        ApiResponse response = statisticsService.getStatistics();
        return new ResponseEntity(response.getResult(), response.getStatus());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        ApiResponse response = indexingService.startIndexing();
        return new ResponseEntity(response.getResult(), response.getStatus());
    }

    @GetMapping({"/stopIndexing", "/ng"})
    public ResponseEntity stopIndexing(HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiResponse response = indexingService.stopIndexing(path);
        return new ResponseEntity(response.getResult(), response.getStatus());
    }
}
