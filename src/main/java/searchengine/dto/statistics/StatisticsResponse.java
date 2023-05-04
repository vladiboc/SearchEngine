package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.anyservice.ApiResponse;

@Getter
@Setter
public class StatisticsResponse extends ApiResponse {

    private StatisticsData statistics;

    public StatisticsResponse() {
        super(true);
    }
}
