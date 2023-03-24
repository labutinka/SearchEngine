package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.BasicResponse;

public interface IndexingService {
    BasicResponse startIndexing();
    BasicResponse stopIndexing();
}
