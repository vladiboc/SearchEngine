package searchengine.dto.anyservice;

import lombok.Getter;

@Getter
public class ErrorResponse extends ApiResponse {

    private final String error;

    public ErrorResponse(String error) {
        super(false);
        this.error = error;
    }

}