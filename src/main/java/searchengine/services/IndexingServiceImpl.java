package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.BasicResponse;
import searchengine.config.*;
import searchengine.controllers.exeptions.ApiException;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final ExtensionsList extensions;
    private final JsoupSettings jsoupSettings;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ErrorsList errorsList;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final PageParser pageParser;
    public static volatile  boolean isInterrupted;
    public static boolean isRunning;

    @Override
    public BasicResponse startIndexing() throws ApiException {
        if (isRunning) {
            throw new ApiException(errorsList.getErrors().get("indexingAlreadyStarted"));
        }
        List<Site> sitesList = sites.getSites();
        setIsRunningWhileStarted();
        setIsInterruptedWhileStarted();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        executeMultiplyIndexing(sitesList, forkJoinPool);

        return new BasicResponse(true);
    }
    @Override
    public BasicResponse stopIndexing() throws ApiException{
        setIsInterruptedWhileStopped();
        if (!isRunning) {
            throw new ApiException(errorsList.getErrors().get("indexingNotStarted"));
        }
        setIsRunningWhileStopped();
        return new BasicResponse(true);
    }
    private static void setIsRunningWhileStarted(){
        isRunning =  true;
    }
    private static void setIsInterruptedWhileStarted(){
        isInterrupted = false;
    }
    private void executeMultiplyIndexing(List<Site> sitesList, ForkJoinPool forkJoinPool) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < sitesList.size(); i++) {
            deleteInfoForSite(sitesList.get(i).getName(), sitesList.get(i).getUrl());
            SiteEntity siteEntity = createSiteEntity(sitesList.get(i).getName(), sitesList.get(i).getUrl());

            try {
                int finalI = i;
                Runnable task = () -> {
                    SiteParser siteParser = new SiteParser(sitesList.get(finalI).getUrl(),
                            pageRepository, siteRepository,
                            siteEntity,
                            extensions, jsoupSettings,pageParser);
                    forkJoinPool.invoke(siteParser);
                    lemmaRepository.saveAll(siteEntity.getLemmaList());

                if (forkJoinPool.submit(siteParser).isDone()) {
                        changeSiteEntity(siteEntity,IndexingStatus.INDEXED,LocalDateTime.now(),"");
                        setIsRunningWhileStopped();
                        System.out.println("Duration: " + (System.currentTimeMillis() - start));

                    }
                    if (isInterrupted) {
                        changeSiteEntity(siteEntity,IndexingStatus.FAILED,LocalDateTime.now(),errorsList.getErrors().get("indexingStopped"));
                    }
                };
                Thread thread = new Thread(task);
                thread.start();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    private synchronized void deleteInfoForSite(String name, String url) {
        SiteEntity site = siteRepository.findByNameAndUrl(name, url);
        if (site != null){

            pageRepository.findAllPagesForSite(site.getId())
                    .forEach(page ->
                            {
                                indexRepository.indexesForPage(page.getId()).forEach(indexRepository::delete);
                                pageRepository.delete(page);
                            }
                    );
            lemmaRepository.lemmasForSite(site.getId()).forEach(lemmaRepository::delete);
            siteRepository.delete(site);
        }

    }

    private synchronized SiteEntity createSiteEntity(String name, String url) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setStatus(IndexingStatus.INDEXING);
        siteEntity.setName(name);
        siteEntity.setUrl(url);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);

        return siteEntity;
    }

    private synchronized void changeSiteEntity(SiteEntity siteEntity, IndexingStatus status, LocalDateTime statusTime, String lastError){
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(statusTime);
        siteEntity.setLastError(lastError);
        siteRepository.save(siteEntity);
    }

    private static void setIsRunningWhileStopped(){
        isRunning = false;
    }
    private static void setIsInterruptedWhileStopped(){
        isInterrupted = true;
    }
}
