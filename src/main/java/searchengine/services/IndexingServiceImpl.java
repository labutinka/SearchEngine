package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.BasicResponse;
import searchengine.config.*;
import searchengine.controllers.exeptions.ApiException;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
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
    public volatile static boolean isInterrupted;
    public static boolean isRunning;

    @Override
    public BasicResponse startIndexing() throws ApiException {
        if (isRunning) {
            throw new ApiException(errorsList.getErrors().get("indexingAlreadyStarted"));
        }
        List<Site> sitesList = sites.getSites();
        isRunning = true;
        isInterrupted = false;
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        executeMultiplyIndexing(sitesList, forkJoinPool);

        return new BasicResponse(true);
    }
    @Override
    public BasicResponse stopIndexing() throws ApiException{
        isInterrupted = true;
        if (!isRunning) {
            throw new ApiException(errorsList.getErrors().get("indexingNotStarted"));
        }
        isRunning = false;
        return new BasicResponse(true);
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

    private synchronized void clearSitePage(String name, String url) {
        siteRepository.findByNameAndUrl(name, url)
                .forEach(siteRepository::delete);
    }

    private void executeMultiplyIndexing(List<Site> sitesList, ForkJoinPool forkJoinPool) {
        for (int i = 0; i < sitesList.size(); i++) {
            clearSitePage(sitesList.get(i).getName(), sitesList.get(i).getUrl());
            SiteEntity siteEntity = createSiteEntity(sitesList.get(i).getName(), sitesList.get(i).getUrl());

            try {
                int finalI = i;
                Runnable task = () -> {
                    SiteParser siteParser = new SiteParser(sitesList.get(finalI).getUrl(),
                            pageRepository, siteRepository,
                            siteEntity,
                            extensions, jsoupSettings);
                    forkJoinPool.invoke(siteParser);
                    if (forkJoinPool.submit(siteParser).isDone()) {
                        changeSiteEntity(siteEntity,IndexingStatus.INDEXED,LocalDateTime.now(),"");
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

    private synchronized void changeSiteEntity(SiteEntity siteEntity, IndexingStatus status, LocalDateTime statusTime, String lastError){
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(statusTime);
        siteEntity.setLastError(lastError);
        siteRepository.save(siteEntity);
    }
}
