package dev.jjcoll.distributedtaskviz;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;


    // For JSON serialization
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSubmitTask() throws Exception {
        // Arrange: Create the DTO
        TaskSubmissionRequestDTO newTask = new TaskSubmissionRequestDTO("new task");

        // Act & Assert: Perform the request and verify the response
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        // Build the request
                        .content(objectMapper.writeValueAsString(newTask)))
                // Assertion
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("new task")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.id").exists())
                .andDo(print());
    }

    @Test
    void testGetTaskSuccess() throws Exception {
        // First, create a task to retrieve later
        TaskSubmissionRequestDTO newTask = new TaskSubmissionRequestDTO("task to retrieve");

        String responseJson = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the ID from the response
        Long taskId = objectMapper.readTree(responseJson).get("id").asLong();

        // Now retrieve the task by its ID
        mockMvc.perform(get("/api/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.name", is("task to retrieve")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andDo(print());
    }

    @Test
    void testGetTaskNotFound() throws Exception {
        // Try to get a task with an ID that doesn't exist
        Long nonExistentId = 99999L;

        mockMvc.perform(get("/api/tasks/" + nonExistentId))
                .andExpect(status().isNotFound())  // 404 status
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Task with id " + nonExistentId + " not found")))
                .andExpect(jsonPath("$.path", is("/api/tasks/" + nonExistentId)))
                .andDo(print());
    }


    @Test
    void testGetTaskWithInvalidIdFormat() throws Exception {
        // Try to get a task with an ID that is not a valid Long
        String invalidId = "looser";

        // Spring will throw MethodArgumentTypeMismatchException
        // Since we don't have a specific handler, it falls back to generic Exception handler
        // which returns 400 Bad Request (type conversion failure)
        mockMvc.perform(get("/api/tasks/" + invalidId))
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andDo(print());
    }



}
