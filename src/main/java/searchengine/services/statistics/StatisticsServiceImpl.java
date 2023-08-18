package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.anyservice.ApiError;
import searchengine.dto.anyservice.ApiResponse;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResult;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.dbconnectors.DbcStatistics;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final DbcStatistics dbcStatistics;
    private final SitesList sites;

    @Override
    public ApiResponse getStatistics() {

        TotalStatistics total = dbcStatistics.requestTotalStatistics();
        if (total == null) {
            return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    new ApiError("SQL ошибка при получении общей статистики для всех сайтов"));
        }

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(Site site : sitesList) {
            DetailedStatisticsItem siteStatistics = dbcStatistics.requestSiteStatistics(site.getName());
            if (siteStatistics == null) {
                return new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        new ApiError("SQL ошибка при получении статистики для сайта " + site.getName()));
            }
            detailed.add(siteStatistics);
        }

        StatisticsResult result = new StatisticsResult();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        result.setStatistics(data);
        return new ApiResponse(HttpStatus.OK, result);
    }
}
