package dev.jjcoll.distributedtaskviz;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jjcoll.distributedtaskviz.controller.TaskController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke tests for the application.
 * These tests verify that the Spring application context loads successfully
 * and that critical beans are created and wired correctly.
 */
// This tells Spring to load the entire application context when running the test
@SpringBootTest
 class BackendApplicationTests {

    // Inject instance of TaskController
    @Autowired
    private TaskController controller;

    /**
     * @Test marks this method as a test method that should be executed by JUnit.
     * When you run tests (via IDE or 'mvn test'):
     * 1. JUnit finds all methods annotated with @Test
     * 2. It runs each test method independently
     * 3. If any assertion fails or an exception is thrown, the test fails
     * 4. If the method completes without errors, the test passes
     *
     * This specific test is a "smoke test" - it verifies the application can start
     * and that Spring successfully creates the TaskController bean.
     */
    @Test
    void contextLoads() {
        // Check that the controller object is not null. If it is null, fail the test
        assertThat(controller).isNotNull();
    }

}
