package searchengine.services;

import searchengine.dto.BasicResponse;
import searchengine.controllers.exeptions.ApiException;

public interface IndexingService {
    BasicResponse startIndexing() throws ApiException;
    BasicResponse stopIndexing() throws ApiException;
}
