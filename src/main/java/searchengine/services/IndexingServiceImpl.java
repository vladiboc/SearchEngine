package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.anyservice.ErrorResponse;

@Service
public class IndexingServiceImpl implements IndexingService {

    public ApiResponse startIndexing() {
        return new ApiResponse(true);
    }

    public ApiResponse stopIndexing() {
        return new ErrorResponse("Так мы ж и не индексируем...");
    }
}
