package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.StatisticsService;

import javax.persistence.Id;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexService;

    public ApiController(StatisticsService statisticsService,IndexingService indexService) {
        this.statisticsService = statisticsService;
        this.indexService = indexService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing(){

        return ResponseEntity.ok(indexService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing(){
        return ResponseEntity.ok(indexService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public void indexPage(String url){

    }

    @GetMapping("/search")
    public void search(String query, String site, int offset, int limit){

    }

}
