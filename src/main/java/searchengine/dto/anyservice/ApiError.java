package searchengine.dto.anyservice;

import lombok.Getter;

@Getter
public class ApiError extends ApiResponseResult {

    private final String error;

    public ApiError(String error) {
        super(false);
        this.error = error;
    }

}