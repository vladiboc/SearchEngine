package searchengine.dto.anyservice;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public class ApiResponse {

    private final HttpStatus status;

    private final ApiResult result;

}
