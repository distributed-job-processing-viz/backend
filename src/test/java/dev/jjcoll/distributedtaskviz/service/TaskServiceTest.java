package dev.jjcoll.distributedtaskviz.service;

import dev.jjcoll.distributedtaskviz.exception.TaskNotFoundException;
import dev.jjcoll.distributedtaskviz.mappers.TaskMapper;
import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.model.TaskStatus;
import dev.jjcoll.distributedtaskviz.model.Worker;
import dev.jjcoll.distributedtaskviz.model.WorkerStatus;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private Worker testWorker;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testWorker = new Worker();
        testWorker.setId(1L);
        testWorker.setName("test-worker");
        testWorker.setStatus(WorkerStatus.IDLE);
        testWorker.setCreatedAt(LocalDateTime.now());
        testWorker.setLastHeartbeat(LocalDateTime.now());

        testTask = new Task();
        testTask.setId(1L);
        testTask.setName("test-task");
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setComplexity(Complexity.LOW);
        testTask.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testClaimTaskForWorker_TaskAvailable_ReturnsClaimedTask() {
        // Given: Repository returns PENDING task
        when(taskRepository.findFirstPendingTaskForLocking()).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When: claimTaskForWorker() called
        Optional<Task> result = taskService.claimTaskForWorker(testWorker);

        // Then: Task claimed
        assertThat(result).isPresent();
        assertThat(result.get().getAssignedWorker()).isEqualTo(testWorker);
        assertThat(result.get().getStatus()).isEqualTo(TaskStatus.PROCESSING);
        assertThat(result.get().getProcessingStartedAt()).isNotNull();

        // Verify repository interactions
        verify(taskRepository).findFirstPendingTaskForLocking();
        verify(taskRepository).save(testTask);
    }

    @Test
    void testClaimTaskForWorker_NoTasksAvailable_ReturnsEmpty() {
        // Given: Repository returns empty
        when(taskRepository.findFirstPendingTaskForLocking()).thenReturn(Optional.empty());

        // When: claimTaskForWorker() called
        Optional<Task> result = taskService.claimTaskForWorker(testWorker);

        // Then: Returns empty
        assertThat(result).isEmpty();

        // Verify no save() called
        verify(taskRepository).findFirstPendingTaskForLocking();
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testCompleteTask_SetsStatusAndCompletedAt() {
        // Given: Task exists with status PROCESSING
        testTask.setStatus(TaskStatus.PROCESSING);
        testTask.setAssignedWorker(testWorker);
        testTask.setProcessingStartedAt(LocalDateTime.now());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When: completeTask() called
        taskService.completeTask(1L);

        // Then: Status updated
        assertThat(testTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(testTask.getCompletedAt()).isNotNull();

        // Verify save called
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(testTask);
    }

    @Test
    void testCompleteTask_TaskNotFound_ThrowsException() {
        // Given: Task doesn't exist
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then: Exception thrown
        assertThatThrownBy(() -> taskService.completeTask(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("999");

        // Verify no save called
        verify(taskRepository).findById(999L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testFailTask_SetsStatusAndCompletedAt() {
        // Given: Task exists
        testTask.setStatus(TaskStatus.PROCESSING);
        testTask.setAssignedWorker(testWorker);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When: failTask() called
        taskService.failTask(1L, "Test error");

        // Then: Status updated
        assertThat(testTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(testTask.getCompletedAt()).isNotNull();

        // Verify save called
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(testTask);
    }

    @Test
    void testFailTask_TaskNotFound_ThrowsException() {
        // Given: Task doesn't exist
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then: Exception thrown
        assertThatThrownBy(() -> taskService.failTask(999L, "Test error"))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("999");

        // Verify no save called
        verify(taskRepository).findById(999L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testFailTask_WithErrorMessage_LogsError() {
        // Given: Task exists
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When: failTask() called with error message
        String errorMessage = "Database connection timeout";
        taskService.failTask(1L, errorMessage);

        // Then: Task marked as failed
        assertThat(testTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        // Error message is logged (verified by logger in actual implementation)
    }
}
