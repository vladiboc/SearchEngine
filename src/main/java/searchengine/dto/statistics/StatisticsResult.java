package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.anyservice.ApiResult;

@Getter
@Setter
public class StatisticsResult extends ApiResult {

    private StatisticsData statistics;

    public StatisticsResult() {
        super(true);
    }

}
