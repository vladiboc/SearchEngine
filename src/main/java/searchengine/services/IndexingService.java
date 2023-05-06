package searchengine.services;

import searchengine.dto.anyservice.ApiResponse;

public interface IndexingService {
    ApiResponse startIndexing();
    ApiResponse stopIndexing();

}
