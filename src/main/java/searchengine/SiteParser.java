package searchengine;

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
    private static Set<String> links = Collections.synchronizedSet(new HashSet<>());
    private static Set<String> parsedLinks = Collections.synchronizedSet(new HashSet<>());
    private final ExtensionsList extensions;
    SiteEntity siteEntity;
    private URI uri;
    protected String rootUrl;
    PageRepository pageRepository;
    SiteRepository siteRepository;
    JsoupSettings jsoupSettings;


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
        while (!isInterrupted) {
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
            siteEntity.setLastError("Ошибка при обработке " + rootUrl + ex.getLocalizedMessage());
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
            try {
                saveErrorPage(rootUrl, ex.getStatusCode());
            } catch (IOException | URISyntaxException exc) {
                exc.printStackTrace();
            }
        } catch (InterruptedException | IOException exc) {
            exc.printStackTrace();
        }
        if (doc != null) {
            Elements urlsAtag = doc.select("a");
            urlsAtag.forEach(href ->
            {
                String url = href.absUrl("href");
                if (checkURL(url) && links.add(url)) {
                    result.add(href.absUrl("href"));
                    try {
                        savePage(href);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
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

    private synchronized void savePage(Element path) throws IOException {
        if (pageRepository.findByName(fixPath(path.attr("href"))) == null) {
            Connection.Response response = Jsoup.connect(path.absUrl("href")).execute();
            int code = response.statusCode();

            PageEntity page = new PageEntity();
            page.setSiteId(siteEntity);
            page.setCode(code);
            page.setContent(response.body());
            page.setPath(fixPath(path.attr("href")));
            pageRepository.save(page);

            updateTimeForSite(siteEntity);
        }

    }

    private synchronized void saveErrorPage(String url, int statusCode) throws IOException, URISyntaxException {

        URI uri = new URI(url);

        if (pageRepository.findByName(uri.getPath()) == null) {
            PageEntity page = new PageEntity();
            page.setSiteId(siteEntity);
            page.setCode(statusCode);
            page.setContent(" ");
            page.setPath(uri.getPath());
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

    private void updateTimeForSite(SiteEntity siteEntity) {
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
    }
}
