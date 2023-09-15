package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.anyservice.ApiResult;
import searchengine.dto.statistics.StatisticsData;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SearchResult extends ApiResult {
    private int count;
    private List<SearchItem> data;

    public SearchResult() {
        super(true);
        this.count = 0;
        this.data = new ArrayList<>();
    }
}
