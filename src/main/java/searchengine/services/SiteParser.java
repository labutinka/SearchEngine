package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ExtensionsList;

import searchengine.config.JsoupSettings;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.net.URI;

import static searchengine.services.IndexingServiceImpl.isInterrupted;


public class SiteParser extends RecursiveTask<Set<String>> {
    private static final Logger logger = LogManager.getLogger(SiteParser.class);
    private static final Set<String> links = Collections.synchronizedSet(new HashSet<>());
    private final ExtensionsList extensions;
    SiteEntity siteEntity;
    private URI uri;
    protected String rootUrl;
    PageRepository pageRepository;
    SiteRepository siteRepository;
    JsoupSettings jsoupSettings;
    private int statusCode;
    private String content;
    private String path;
    private static final Set<String> parsedLinks = Collections.synchronizedSet(new HashSet<>());

    public SiteParser(String rootUrl, PageRepository pageRepository, SiteRepository siteRepository,
                      SiteEntity siteEntity, ExtensionsList extensions, JsoupSettings jsoupSettings) {
        this.rootUrl = rootUrl;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
        this.extensions = extensions;
        this.jsoupSettings = jsoupSettings;
        logger.info("Cоздан экземпляр для ссылки: " + rootUrl);
    }

    @Override
    protected Set<String> compute() {
        List<SiteParser> taskList = new ArrayList<>();
        if (!isInterrupted) {
            try {
                uri = new URI(rootUrl);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            for (String singleLink : getUrl(rootUrl)) {
                if (!parsedLinks.contains(singleLink)) {
                    SiteParser task = new SiteParser(singleLink, pageRepository, siteRepository, siteEntity, extensions, jsoupSettings);
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
            parseElements(urlsAtag, result);
        }

        return result;
    }

    private boolean checkURL(String url) {
        boolean isUrlOk = true;
        for (int i = 0; i < extensions.getExtensions().size(); i++) {
            if (url.contains(extensions.getExtensions().get(i))) {
                isUrlOk = false;
                break;
            }
        }
        return url.contains(uri.getHost()) && isUrlOk;
    }

    private void saveDataPage(Element linkPath) throws IOException {
        Connection.Response response = Jsoup.connect(linkPath.absUrl("href")).execute();
        statusCode = response.statusCode();
        content = response.body();
        path = fixPath(linkPath.attr("href"));
    }

    private void saveDataErrorPage(String url, int code) throws IOException, URISyntaxException {
        URI uri = new URI(url);
        statusCode = code;
        content = " ";
        path = uri.getPath();
    }

    private synchronized void savePage(SiteEntity siteEntity, int code, String content, String path) throws IOException {
        if (pageRepository.findByName(path) == null) {
            PageEntity page = new PageEntity();
            page.setSiteId(siteEntity);
            page.setCode(code);
            page.setContent(content);
            page.setPath(path);
            pageRepository.save(page);
            updateTimeForSite(siteEntity);
        }
    }

    private String fixPath(String path) {
        if (path.startsWith("http")) {
            try {
                URI uri = new URI(path);
                return uri.getPath();
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
            }
        }
        return path;
    }

    private synchronized void updateTimeForSite(SiteEntity siteEntity) {
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
    }

    private void manageErrorPage(HttpStatusException ex) {
        siteEntity.setLastError("Ошибка при обработке " + rootUrl + ex.getLocalizedMessage());
        updateTimeForSite(siteEntity);
        try {
            saveDataErrorPage(rootUrl, ex.getStatusCode());
            savePage(siteEntity, statusCode, content, path);
        } catch (IOException | URISyntaxException exc) {
            exc.printStackTrace();
        }
    }

    private void parseElements(Elements urlsAtag, TreeSet<String> result){
        urlsAtag.forEach(href ->
        {
            String url = href.absUrl("href");
            if (checkURL(url) && links.add(url)) {
                result.add(href.absUrl("href"));
                try {
                    saveDataPage(href);
                    savePage(siteEntity, statusCode, content, path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
