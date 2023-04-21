package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.BasicResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.controllers.exeptions.ApiException;
import searchengine.services.IndexPageService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;



@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexService;
     private final IndexPageService indexPageService;


    public ApiController(StatisticsService statisticsService,IndexingService indexService,
                                              IndexPageService indexPageService) {
        this.statisticsService = statisticsService;
        this.indexService = indexService;
        this.indexPageService = indexPageService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<BasicResponse> startIndexing() throws ApiException {
        return ResponseEntity.ok(indexService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<BasicResponse> stopIndexing() throws ApiException {
        return ResponseEntity.ok(indexService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<BasicResponse> indexPage(String url) throws ApiException{
        return ResponseEntity.ok(indexPageService.indexPage(url));
    }

    @GetMapping("/search")
    public void search(String query, String site, int offset, int limit){

    }

}
