package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.controllers.exeptions.ApiException;
import searchengine.dto.BasicResponse;

@Service
public interface IndexPageService {
     BasicResponse indexPage(String url) throws ApiException;
}
