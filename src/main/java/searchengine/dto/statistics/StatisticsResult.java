package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.anyservice.ApiResponseResult;

@Getter
@Setter
public class StatisticsResult extends ApiResponseResult {

    private StatisticsData statistics;

    public StatisticsResult() {
        super(true);
    }
}
