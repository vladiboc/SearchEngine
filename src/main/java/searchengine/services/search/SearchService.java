package searchengine.services.search;

import org.springframework.lang.Nullable;
import searchengine.dto.anyservice.ApiResponse;

public interface SearchService {

    ApiResponse search(
            @Nullable String site, @Nullable String query, @Nullable String offset, @Nullable String limit
    );

}
