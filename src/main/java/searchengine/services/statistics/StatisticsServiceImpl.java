package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResult;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.dbconnectors.DbcStatistics;
import searchengine.model.entities.IndexedSite;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final DbcStatistics dbcStatistics;

    @Override
    public ApiResponse getStatistics() {
        TotalStatistics total = dbcStatistics.requestTotalStatistics();
        if (total == null) {
            return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    new ApiError("SQL ошибка при получении общей статистики для всех сайтов"));
        }
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<IndexedSite> indexedSitesList = dbcStatistics.requestIndexedSites();
        for(IndexedSite indexedSite : indexedSitesList) {
            DetailedStatisticsItem siteStatistics = dbcStatistics.requestDetailedSiteStatistics(indexedSite);
            if (siteStatistics == null) {
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        new ApiError("SQL ошибка при получении статистики для сайта " + indexedSite.getName()));
            }
            detailed.add(siteStatistics);
        }
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        StatisticsResult result = new StatisticsResult();
        result.setStatistics(data);
        return new ApiResponse(HttpStatus.OK, result);
    }
}