package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ErrorsList;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.exeptions.ApiException;
import searchengine.dto.BasicResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexPageServiceImpl implements IndexPageService {
    private final ErrorsList errorsList;
    private final SitesList sites;
    private final PageParser pageParser;

    @Override
    public BasicResponse indexPage(String url) throws ApiException {

        if (StringUtils.isEmpty(url) || !checkIsCorrect(url)) {
            throw new ApiException(errorsList.getErrors().get("pageOutOfBound"));
        }
        pageParser.parsePage(url);

        return new BasicResponse(true);
    }

    private boolean checkIsCorrect(String url) {
        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }
}
