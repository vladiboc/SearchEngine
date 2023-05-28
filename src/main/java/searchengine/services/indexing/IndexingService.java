package searchengine.services.indexing;

import searchengine.dto.anyservice.ApiResponse;

public interface IndexingService {
    ApiResponse startIndexing();
    ApiResponse stopIndexing();

}