package searchengine.dto;

import lombok.Value;

@Value
public class DetailedSearchItem {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    float relevance;

}
