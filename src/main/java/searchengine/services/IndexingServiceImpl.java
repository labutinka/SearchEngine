package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.BasicResponse;
import searchengine.config.*;
import searchengine.controllers.exeptions.ApiException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;


@Service
@Slf4j
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
    public static volatile boolean isInterrupted;
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
    public BasicResponse stopIndexing() throws ApiException {
        setIsInterruptedWhileStopped();
        if (!isRunning) {
            throw new ApiException(errorsList.getErrors().get("indexingNotStarted"));
        }
        setIsRunningWhileStopped();
        return new BasicResponse(true);
    }

    private static void setIsRunningWhileStarted() {
        isRunning = true;
    }

    private static void setIsInterruptedWhileStarted() {
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
                    SiteParser siteParser = new SiteParser(sitesList.get(finalI).getUrl(), sitesList.get(finalI).getUrl(),
                            pageRepository, siteRepository,
                            siteEntity,
                            extensions, jsoupSettings, pageParser);
                    forkJoinPool.invoke(siteParser);

                    if (forkJoinPool.submit(siteParser).isDone()) {
                        changeSiteEntity(siteEntity, IndexingStatus.INDEXED, LocalDateTime.now(), "");
                        setIsRunningWhileStopped();
                        log.info("Duration: " + (System.currentTimeMillis() - start));
                    }
                    if (isInterrupted) {
                        changeSiteEntity(siteEntity, IndexingStatus.FAILED, LocalDateTime.now(), errorsList.getErrors().get("indexingStopped"));
                    }
                };
                Thread thread = new Thread(task);
                thread.start();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            pageRepository.deactivateIndex();
        }
    }

    private synchronized void deleteInfoForSite(String name, String url) {
        Optional<SiteEntity> site = siteRepository.findByNameAndUrl(name, url);
        ArrayList<IndexEntity> indexToDelete = new ArrayList<>();
        ArrayList<PageEntity> pageToDelete = new ArrayList<>();
        if (site.isPresent()) {
            pageRepository.findAllPagesForSite(site.get().getId())
                    .forEach(page ->
                            {
                                indexToDelete.addAll(indexRepository.indexesForPage(page.getId()));
                                indexRepository.deleteAll(indexToDelete);
                                pageToDelete.add(page);
                            }
                    );

            pageRepository.deleteAll(pageToDelete);
            lemmaRepository.deleteAll(new ArrayList<>(lemmaRepository.lemmasForSite(site.get().getId())));
            siteRepository.delete(site.get());
        }
    }

    private SiteEntity createSiteEntity(String name, String url) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setStatus(IndexingStatus.INDEXING);
        siteEntity.setName(name);
        siteEntity.setUrl(url);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
        return siteEntity;
    }

    private synchronized void changeSiteEntity(SiteEntity siteEntity, IndexingStatus status, LocalDateTime statusTime, String lastError) {
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(statusTime);
        siteEntity.setLastError(lastError);
        siteRepository.saveAndFlush(siteEntity);
    }

    private static void setIsRunningWhileStopped() {
        isRunning = false;
    }

    private static void setIsInterruptedWhileStopped() {
        isInterrupted = true;
    }
}
