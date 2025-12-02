package dev.jjcoll.distributedtaskviz.config;

import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with initial task data on application startup.
 * Only runs when the 'test' profile is NOT active to avoid interference with unit tests.
 * Uses the actual TaskService submission flow to ensure consistency with production behavior.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final TaskService taskService;

    public DataSeeder(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void run(String... args) {
        logger.info("Seeding database with initial task data...");

        // Seed tasks with varying complexities
        seedTask("Process customer payment batch", Complexity.HIGH);
        seedTask("Send welcome email to new users", Complexity.LOW);
        seedTask("Generate monthly analytics report", Complexity.MEDIUM);
        seedTask("Backup user database", Complexity.HIGH);
        seedTask("Update product catalog cache", Complexity.LOW);
        seedTask("Validate shipping addresses", Complexity.MEDIUM);
        seedTask("Calculate tax for orders", Complexity.MEDIUM);
        seedTask("Resize uploaded images", Complexity.LOW);
        seedTask("Run machine learning model training", Complexity.HIGH);
        seedTask("Clean up temporary files", Complexity.LOW);

        logger.info("Database seeding completed successfully");
    }

    private void seedTask(String name, Complexity complexity) {
        TaskSubmissionRequestDTO taskDTO = new TaskSubmissionRequestDTO(name, complexity);
        taskService.submitTask(taskDTO);
        logger.debug("Seeded task: {} ({})", name, complexity);
    }
}
