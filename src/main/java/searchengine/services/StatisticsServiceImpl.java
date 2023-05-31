package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private int pages = 0;
    private int lemmas = 0;
    private String status = " ";
    private String error = " ";
    private long millis = 0L;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Optional<SiteEntity> siteEntity = siteRepository.findByNameAndUrl(site.getName(), site.getUrl());
            siteEntity.ifPresent(this::updateDataForSite);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(status);
            item.setError(error);
            item.setStatusTime(millis);
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
            clearData();
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private void updateDataForSite(SiteEntity siteEntity){
        if (siteEntity != null) {
            pages = pageRepository.countPagesForSite(siteEntity.getId());
            lemmas = lemmaRepository.countLemmasForSite(siteEntity.getId());
            status = siteEntity.getStatus().toString();
            error = siteEntity.getLastError();
            Timestamp timestamp = Timestamp.valueOf(siteEntity.getStatusTime());
            millis = timestamp.getTime();
        }
    }
    private void clearData(){
        pages = 0;
        lemmas = 0;
        error = " ";
        status = " ";
        millis = 0L;
    }

}
