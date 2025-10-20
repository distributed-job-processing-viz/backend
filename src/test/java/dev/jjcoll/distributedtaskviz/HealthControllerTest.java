package dev.jjcoll.distributedtaskviz;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class HealthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())  // Verify 200 OK status
                .andExpect(content().string("Api up and running healthy."))  // Verify response body
                .andDo(print());  // Print request/response for debugging
    }
}
