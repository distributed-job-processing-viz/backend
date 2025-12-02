package dev.jjcoll.distributedtaskviz;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.model.Complexity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for TaskController endpoints.
 *
 * @ActiveProfiles("test") activates the "test" profile, which prevents the DataSeeder
 * from running (it has @Profile("!test")). This ensures tests start with an empty database.
 *
 * @Transactional ensures test isolation by wrapping each test method in a transaction
 * that is automatically rolled back after the test completes. This means:
 * - Each test starts with a clean database state
 * - Data created in one test doesn't affect other tests
 * - No manual cleanup needed between tests
 * - Much faster than recreating the Spring context (@DirtiesContext)
 *
 * How it works:
 * 1. Test starts → Transaction begins
 * 2. Test runs → Database changes are made
 * 3. Test ends → Transaction rolls back (changes discarded)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;


    // For JSON serialization
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSubmitTask() throws Exception {
        // Arrange: Create the DTO
        TaskSubmissionRequestDTO newTask = new TaskSubmissionRequestDTO("new task", Complexity.HIGH);

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
        TaskSubmissionRequestDTO newTask = new TaskSubmissionRequestDTO("task to retrieve", Complexity.LOW);

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

    // ========== Tests for GET /api/tasks (retrieveAll) ==========

    @Test
    void testGetAllTasks_NoFilters() throws Exception {
        // Arrange: Create sample tasks with different statuses and complexities
        createTask("Task 1", Complexity.LOW);
        createTask("Task 2", Complexity.MEDIUM);
        createTask("Task 3", Complexity.HIGH);

        // Act & Assert: Get all tasks without filters
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(20)) // Default page size
                .andExpect(jsonPath("$.number").value(0)) // First page
                .andDo(print());
    }

    @Test
    void testGetAllTasks_FilterByStatus() throws Exception {
        // Arrange: Create tasks - all will have status PENDING due to mapper
        createTask("Pending Task 1", Complexity.LOW);
        createTask("Pending Task 2", Complexity.MEDIUM);
        createTask("Pending Task 3", Complexity.HIGH);

        // Act & Assert: Filter by PENDING status
        mockMvc.perform(get("/api/tasks?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PENDING"))))
                .andDo(print());
    }

    @Test
    void testGetAllTasks_FilterByComplexity() throws Exception {
        // Arrange: Create tasks with different complexities
        createTask("Low Task 1", Complexity.LOW);
        createTask("Low Task 2", Complexity.LOW);
        createTask("Medium Task", Complexity.MEDIUM);
        createTask("High Task", Complexity.HIGH);

        // Act & Assert: Filter by LOW complexity
        mockMvc.perform(get("/api/tasks?complexity=LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].complexity", everyItem(is("LOW"))))
                .andDo(print());
    }

    @Test
    void testGetAllTasks_FilterByStatusAndComplexity() throws Exception {
        // Arrange: Create diverse tasks
        createTask("Low Pending 1", Complexity.LOW);
        createTask("Low Pending 2", Complexity.LOW);
        createTask("Medium Pending", Complexity.MEDIUM);
        createTask("High Pending", Complexity.HIGH);

        // Act & Assert: Filter by PENDING status AND LOW complexity
        mockMvc.perform(get("/api/tasks?status=PENDING&complexity=LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PENDING"))))
                .andExpect(jsonPath("$.content[*].complexity", everyItem(is("LOW"))))
                .andDo(print());
    }

    @Test
    void testGetAllTasks_Pagination() throws Exception {
        // Arrange: Create 5 tasks
        for (int i = 1; i <= 5; i++) {
            createTask("Task " + i, Complexity.LOW);
        }

        // Act & Assert: Request page 0 with size 2
        mockMvc.perform(get("/api/tasks?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andDo(print());

        // Request page 1 with size 2
        mockMvc.perform(get("/api/tasks?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andDo(print());

        // Request page 2 with size 2 (should have 1 item)
        mockMvc.perform(get("/api/tasks?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.number").value(2))
                .andDo(print());
    }

    @Test
    void testGetAllTasks_Sorting() throws Exception {
        // Arrange: Create tasks with slight delays to ensure different timestamps
        createTask("First Task", Complexity.LOW);
        Thread.sleep(10);
        createTask("Second Task", Complexity.MEDIUM);
        Thread.sleep(10);
        createTask("Third Task", Complexity.HIGH);

        // Act & Assert: Default sort is createdAt DESC (newest first)
        mockMvc.perform(get("/api/tasks?sort=createdAt,DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Third Task"))
                .andExpect(jsonPath("$.content[2].name").value("First Task"))
                .andDo(print());

        // Sort by createdAt ASC (oldest first)
        mockMvc.perform(get("/api/tasks?sort=createdAt,ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("First Task"))
                .andExpect(jsonPath("$.content[2].name").value("Third Task"))
                .andDo(print());
    }

    @Test
    void testGetAllTasks_EmptyResult() throws Exception {
        // Don't create any tasks

        // Act & Assert: Should return empty page
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.empty").value(true))
                .andDo(print());
    }

    @Test
    void testGetAllTasks_NoMatchingFilter() throws Exception {
        // Arrange: Create only LOW complexity tasks
        createTask("Low Task 1", Complexity.LOW);
        createTask("Low Task 2", Complexity.LOW);

        // Act & Assert: Filter by HIGH complexity (should return empty)
        mockMvc.perform(get("/api/tasks?complexity=HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andDo(print());
    }

    // ========== Performance Tests ==========

    @Test
    @Tag("performance")
    void testGetAllTasks_PerformanceWith1000Tasks() throws Exception {
        System.out.println("=== Performance Test: Creating 1000 tasks ===");

        // Arrange: Create 1000 tasks with varied complexities
        long setupStart = System.currentTimeMillis();
        for (int i = 1; i <= 1000; i++) {
            Complexity complexity = switch (i % 3) {
                case 0 -> Complexity.LOW;
                case 1 -> Complexity.MEDIUM;
                default -> Complexity.HIGH;
            };
            createTask("Performance Task " + i, complexity);

            // Progress indicator
            if (i % 100 == 0) {
                System.out.println("Created " + i + " tasks...");
            }
        }
        long setupEnd = System.currentTimeMillis();
        System.out.println("Setup completed in: " + (setupEnd - setupStart) + "ms");

        // Test 1: Get first page with default settings
        System.out.println("\n=== Test 1: Get first page (20 items) ===");
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/tasks?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1000))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalPages").value(50));
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("First page query took: " + duration + "ms");
        assert duration < 1000 : "First page query took too long: " + duration + "ms";

        // Test 2: Get middle page
        System.out.println("\n=== Test 2: Get middle page (page 25) ===");
        startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/tasks?page=25&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.number").value(25));
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Middle page query took: " + duration + "ms");
        assert duration < 1000 : "Middle page query took too long: " + duration + "ms";

        // Test 3: Get last page
        System.out.println("\n=== Test 3: Get last page (page 49) ===");
        startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/tasks?page=49&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.last").value(true));
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Last page query took: " + duration + "ms");
        assert duration < 1000 : "Last page query took too long: " + duration + "ms";

        // Test 4: Filter by complexity (should return ~333 tasks)
        System.out.println("\n=== Test 4: Filter by LOW complexity ===");
        startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/tasks?complexity=LOW&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].complexity", everyItem(is("LOW"))))
                .andExpect(jsonPath("$.content.length()").value(20));
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Filtered query took: " + duration + "ms");
        assert duration < 1000 : "Filtered query took too long: " + duration + "ms";

        // Test 5: Large page size
        System.out.println("\n=== Test 5: Get 100 items in one page ===");
        startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/tasks?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(100))
                .andExpect(jsonPath("$.totalPages").value(10));
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        System.out.println("Large page query took: " + duration + "ms");
        assert duration < 1500 : "Large page query took too long: " + duration + "ms";

        System.out.println("\n=== Performance Test Completed Successfully ===");
    }

    // Helper method to create tasks
    private void createTask(String name, Complexity complexity) throws Exception {
        TaskSubmissionRequestDTO task = new TaskSubmissionRequestDTO(name, complexity);
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isCreated());
    }

}
