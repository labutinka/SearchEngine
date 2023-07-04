package searchengine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import searchengine.controllers.ApiController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

}
