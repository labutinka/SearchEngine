package searchengine.dto;

import lombok.Value;

import java.util.List;

@Value
public class SearchResponse {
    boolean result;
    int count;
    List<DetailedSearchItem> data;
}
