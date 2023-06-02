package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import searchengine.config.ErrorsList;
import searchengine.controllers.exeptions.ApiException;
import searchengine.dto.DetailedSearchItem;
import searchengine.dto.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ErrorsList errorsList;
    private final LemmaService lemmaService;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    public SearchResponse findWords(String query, @RequestParam(required = false) String site,
                                    @RequestParam(required = false) int offset,
                                    @RequestParam(required = false) int limit) throws ApiException {
        if (StringUtils.isBlank(query)) {
            throw new ApiException(errorsList.getErrors().get("emptyQuery"));
        }
        pageRepository.activateIndex();
        Map<String, Integer> lemmas = lemmaService.collectLemmas(query);
        ArrayList<String> allLemmasFromQuery = new ArrayList<>(lemmas.keySet());
        ArrayList<Long> pageIds = new ArrayList<>();
        ArrayList<DetailedSearchItem> data = new ArrayList<>();
        List<PageEntity> resultPages;
        Set<SiteEntity> sitesForSearch = getSiteEntitiesForSearch(site);
        checkSiteEntities(sitesForSearch, allLemmasFromQuery);
        ArrayList<PageEntity> pagesForResponse = new ArrayList<>();
        for (SiteEntity singleSite : sitesForSearch) {
            ArrayList<LemmaEntity> lemmasByFrequency = lemmaRepository.findLemmasAndFrequency(allLemmasFromQuery, singleSite.getId(), pageRepository.countPagesForSite(singleSite.getId()) * 9 / 10);
            if (lemmasByFrequency.size() > 0) {
                List<IndexEntity> indexList = indexRepository.findPagesByLemma(lemmasByFrequency.get(0).getId());
                indexList.forEach(index -> pageIds.add(index.getPageId().getId()));
                findPagesIds(lemmasByFrequency, pageIds);
                resultPages = pageRepository.findPagesById(pageIds);
                pagesForResponse.addAll(resultPages);
            }
        }
        getData(data, pagesForResponse, allLemmasFromQuery, query);
        data.sort(Collections.reverseOrder((o1, o2) -> Float.compare(o1.getRelevance(), o2.getRelevance())));
        List<DetailedSearchItem> pageableData = data.stream().skip(offset).limit(limit).toList();
        return new SearchResponse(true, pagesForResponse.size(), pageableData);
    }

    private Set<SiteEntity> getSiteEntitiesForSearch(String site) {
        Set<SiteEntity> sites = new HashSet<>();
        if (StringUtils.isBlank(site)) {
            sites = siteRepository.findIndexedSites();
        } else {
            sites.add(siteRepository.findSiteEntityByUrlContaining(site));
        }
        return sites;
    }

    private void checkSiteEntities(Set<SiteEntity> sitesForSearch, List<String> allLemmasFromQuery) {
        for (Iterator<SiteEntity> iterator = sitesForSearch.iterator(); iterator.hasNext(); ) {
            for (String lemmaQuery : allLemmasFromQuery) {
                if (lemmaRepository.findLemmaByNameAndSiteId(iterator.next().getId(), lemmaQuery).isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    private void findPagesIds(ArrayList<LemmaEntity> lemmasByFrequency, ArrayList<Long> pageIds) {
        for (int i = 1; i < lemmasByFrequency.size(); i++) {
            List<IndexEntity> nextLemmaList = indexRepository.findPagesByLemma(lemmasByFrequency.get(i).getId());
            List<Long> pageIdsNextLemma = new ArrayList<>();
            nextLemmaList.forEach(index -> pageIdsNextLemma.add(index.getPageId().getId()));
            pageIds.retainAll(pageIdsNextLemma);
        }
    }

    private String findTitle(String content) {
        String string = "";
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select("title");
        StringBuilder builder = new StringBuilder();
        elements.forEach(element -> builder.append(element.text()).append(" "));
        if (!builder.isEmpty()) {
            string = builder.toString();
        }
        return string.trim();
    }

    private String createSnippet(String cleanText, String query) {
        Map<String, String> wordToLemma = lemmaService.getLemmaSet(cleanText);
        Map<String, String> queryToLemma = lemmaService.getLemmaSet(query);

        Set<String> resultWords = new HashSet<>();
        for (Map.Entry<String, String> entry : queryToLemma.entrySet()) {
            resultWords.addAll(getKeys(wordToLemma, entry.getValue()));
        }
        resultWords.addAll(queryToLemma.keySet());
        resultWords.addAll(queryToLemma.values());
        List<String> strings = Arrays.stream(cleanText.split("\\.")).toList();
        String snippet = substring(resultWords, strings);

        return makeTextBold(snippet, resultWords);
    }

    private String substring(Set<String> searchWords, List<String> lines) {
        StringBuilder subString = new StringBuilder();
        for (String searchWord : searchWords) {
            for (String line : lines) {
                if (line.toLowerCase().contains(searchWord)) {
                    String splitText = getSplitText(searchWord, line.toLowerCase());
                    subString.append(splitText);
                    break;
                }
            }
        }
        return subString.toString();
    }

    private String getSplitText(String word, String cleanText) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = getMatcher(word, cleanText);
        if (matcher.find()) {
            builder.append(matcher.group(0).trim()).append(" ... ");
        }

        return builder.toString();
    }

    private Matcher getMatcher(String word, String cleanText) {
        String regex = "\\b" + "(.){0,50}" + word + "|" + word + "(.){0,50}";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(cleanText);
    }

    private Set<String> getKeys(Map<String, String> map, String value) {
        return map.entrySet().stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private String makeTextBold(String cleanText, Set<String> resultWords) {
        return Arrays.stream(cleanText.split("\\s")).map(oneWord -> {
            for (String word : resultWords) {
                if (oneWord.toLowerCase().contains(word)) {
                    return "<b>".concat(oneWord).concat("</b>");
                }
            }
            return oneWord;
        }).collect(Collectors.joining(" "));

    }


    private float getAbsRelevanceForPage(PageEntity page, ArrayList<String> lemmasFromQuery) {
        ArrayList<Long> lemmasIdsFromQuery = lemmaRepository.findLemmaIdByName(lemmasFromQuery);
        return indexRepository.countAbsRelevanceForPage(page.getId(), lemmasIdsFromQuery);
    }

    private ArrayList<DetailedSearchItem> getData(ArrayList<DetailedSearchItem> data, ArrayList<PageEntity> pagesForResponse, ArrayList<String> allLemmasFromQuery,
                                                  String query) {
        float maxRelevance;
        if (pagesForResponse.size() > 0) {
            maxRelevance = pagesForResponse.stream().map(page -> getAbsRelevanceForPage(page, allLemmasFromQuery)).toList().stream().max(Float::compareTo).get();
            pagesForResponse.forEach(page -> {
                float relevance = getAbsRelevanceForPage(page, allLemmasFromQuery);
                data.add(buildDataForResponse(page, query, relevance / maxRelevance));
            });
        }
        return data;
    }

    private DetailedSearchItem buildDataForResponse(PageEntity resultPage, String query, float relevance) {
        return new DetailedSearchItem
                (resultPage.getSiteId().getUrl().replaceFirst("/$", ""),
                        resultPage.getSiteId().getName(),
                        resultPage.getPath(), findTitle(resultPage.getContent()),
                        createSnippet(lemmaService.clearContent(resultPage), query),
                        relevance);
    }
}
