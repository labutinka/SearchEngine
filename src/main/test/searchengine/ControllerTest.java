package searchengine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import searchengine.controllers.ApiController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc

public class ControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApiController controller;

    @Test
    public void testStartIndexing() throws Exception{
        this.mockMvc.perform(get("/api/startIndexing")).andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    public void testStopIndexing() throws Exception{
        this.mockMvc.perform(get("/api/stopIndexing")).andExpect(status().is4xxClientError());

    }

    @Test
    public void testIndexPageNegative() throws Exception{
        this.mockMvc.perform(post("/api/indexPage").param("url","http://test.ru"))
                .andExpect(status().is4xxClientError());

    }
    @Test
    public void testIndexPagePositive() throws Exception{
        this.mockMvc.perform(post("/api/indexPage").param("url", "https://www.mail.ru/"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSearch() throws Exception{
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("query", "хорошая книга");
        params.add("offset", "0");
        params.add("limit","5");
        this.mockMvc.perform(get("/api/search").params(params)).andExpect(status().isOk());
    }

    @Test
    public void testStatistics() throws Exception{
        this.mockMvc.perform(get("/api/statistics")).andExpect(status().isOk());
    }
}
