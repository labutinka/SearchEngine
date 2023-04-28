package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.ExtensionsList;
import searchengine.config.JsoupSettings;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.net.URI;

import static searchengine.services.IndexingServiceImpl.isInterrupted;


public class SiteParser extends RecursiveTask<Set<String>> {
    private static final Logger logger = LogManager.getLogger(SiteParser.class);
    private static final Set<String> links = Collections.synchronizedSet(new HashSet<>());
    private final ExtensionsList extensions;
    SiteEntity siteEntity;
    private URI uriForRootUrl;
    protected String rootUrl;
    PageRepository pageRepository;
    SiteRepository siteRepository;
    PageParser pageParser;
    JsoupSettings jsoupSettings;

    private static final Set<String> parsedLinks = Collections.synchronizedSet(new HashSet<>());

    public SiteParser(String rootUrl, PageRepository pageRepository, SiteRepository siteRepository,
                      SiteEntity siteEntity, ExtensionsList extensions, JsoupSettings jsoupSettings,
                      PageParser pageParser) {
        this.rootUrl = rootUrl;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
        this.extensions = extensions;
        this.jsoupSettings = jsoupSettings;
        this.pageParser = pageParser;
        logger.info("Cоздан экземпляр для ссылки: {}" , rootUrl);
    }

    @Override
    protected Set<String> compute() {
        List<SiteParser> taskList = new ArrayList<>();
        if (!isInterrupted) {
            try {
                uriForRootUrl = new URI(rootUrl);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            for (String singleLink : getUrl(rootUrl)) {
                if (!parsedLinks.contains(singleLink)) {
                    SiteParser task = new SiteParser(singleLink, pageRepository, siteRepository,
                            siteEntity, extensions, jsoupSettings, pageParser);
                    taskList.add(task);
                    task.fork();
                }
            }
            for (SiteParser task : taskList) {
                links.addAll(task.join());
            }
        }
        if (isInterrupted) {
            taskList.clear();
        }

        return links;
    }

    private TreeSet<String> getUrl(String rootUrl) {
        Document doc = null;
        TreeSet<String> result = new TreeSet<>();

        try {
            Thread.sleep(150);
            String userAgent = jsoupSettings.getUserAgent();
            String referrer = jsoupSettings.getReferrer();

            doc = Jsoup.connect(rootUrl).userAgent(userAgent)
                    .followRedirects(false)
                    .referrer(referrer)
                    .get();
            parsedLinks.add(rootUrl);

        } catch (HttpStatusException ex) {
            manageErrorPage(ex);
        } catch (InterruptedException | IOException exc) {
            exc.printStackTrace();
        }

        Document finalDoc = doc;
        if (finalDoc != null) {
            Elements urlsAtag = finalDoc.select("a");
            parseElementsSavePage(urlsAtag, result);
        }


        return result;
    }
       private void manageErrorPage(HttpStatusException ex) {
        siteEntity.setLastError("Ошибка при обработке " + rootUrl + ex.getLocalizedMessage());

        try {
            saveErrorPage(siteEntity, ex.getStatusCode(), rootUrl);
        } catch (IOException | URISyntaxException exc) {
            exc.printStackTrace();
        }
    }

    private synchronized void saveErrorPage(SiteEntity siteEntity, int code, String rootUrl) throws IOException, URISyntaxException {
        URI uri = new URI(rootUrl);
        if (pageRepository.findByName(uri.getPath()) == null) {
            PageEntity page = new PageEntity();
            pageParser.setFieldsToPage(page, siteEntity,code," ",uri.getPath());
            pageParser.updateTimeForSite(siteEntity);
        }
    }

    private void parseElementsSavePage(Elements urlsAtag, TreeSet<String> result) {
        urlsAtag.forEach(href ->
        {
            String url = href.absUrl("href");
            if (checkURL(url) && links.add(url)) {
                result.add(url);
                pageParser.parsePage(url);
            }
        });
    }
    private boolean checkURL(String url) {
        boolean isUrlOk = true;
        for (int i = 0; i < extensions.getExtensions().size(); i++) {
            if (url.contains(extensions.getExtensions().get(i))) {
                isUrlOk = false;
                break;
            }
        }
        return url.contains(uriForRootUrl.getHost()) && isUrlOk;
    }
}
