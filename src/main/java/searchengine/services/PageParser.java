package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class PageParser {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;

    private final LemmaService lemmaService;

    public void parsePage(String pageUrl) {
        try {
            Connection.Response response = Jsoup.connect(pageUrl).execute();
            URI siteUrl = new URI(pageUrl);
            int statusCode = response.statusCode();
            String content = response.body();
            String path = siteUrl.getPath();
            updatePage(statusCode, content, path, siteUrl.getHost());

        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        }

    }

    private void updatePage(int code, String content, String path, String pageUrl) {
        SiteEntity siteForPage = findSiteEntity(pageUrl);
        PageEntity page = pageRepository.findByPathAndId(path, siteForPage.getId());
        if (page == null){
            page = new PageEntity();
        }
        setFieldsToPage(page, findSiteEntity(pageUrl), code, content,path);

        String clearedContent = lemmaService.clearContent(page);
        HashMap<String, Integer> lemmasList = lemmaService.collectLemmas(clearedContent);
        for (Map.Entry<String , Integer> entry:  lemmasList.entrySet()){
            String lemma = entry.getKey();
            Integer rank = entry.getValue();
            LemmaEntity lemmaEntity = createLemma(siteForPage, lemma);
            createIndex(lemmaEntity, page, rank);
        }

    }

    private SiteEntity findSiteEntity(String pageUrl) {
        return siteRepository.findSiteEntityByUrlContaining(pageUrl);
    }

    protected synchronized void setFieldsToPage(PageEntity page, SiteEntity siteEntity, int code, String content, String path){
        page.setSiteId(siteEntity);
        page.setCode(code);
        page.setContent(content);
        page.setPath(path);
        pageRepository.save(page);
        updateTimeForSite(siteEntity);
    }
    protected synchronized void updateTimeForSite(SiteEntity siteEntity) {
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
    }

    private LemmaEntity createLemma(SiteEntity siteForPage, String lemma) {
        LemmaEntity lemmaEntity = lemmaRepository.findLemmaByNameAndSiteId(siteForPage.getId(), lemma);
        if (lemmaEntity != null) {
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            lemmaRepository.save(lemmaEntity);
            return lemmaEntity;
        } else {
            LemmaEntity currentLemma = new LemmaEntity();
            currentLemma.setFrequency(1);
            currentLemma.setSiteId(siteForPage);
            currentLemma.setLemma(lemma);
            lemmaRepository.save(currentLemma);
            return  currentLemma;
        }
    }

    private void createIndex(LemmaEntity lemma, PageEntity page, int rank){
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageId(page);
        indexEntity.setLemmaId(lemma);
        indexEntity.setRank(rank);
        indexRepository.save(indexEntity);
    }
}