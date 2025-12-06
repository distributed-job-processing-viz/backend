package dev.jjcoll.distributedtaskviz.repository;


import dev.jjcoll.distributedtaskviz.model.Task;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Making Task repository extend JpaSpecificationExecutor
 * gives us ONE powerful method: taskRepository.findAll(filter, pageAndSort)
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * Finds and locks the first available PENDING task for a worker to claim.
     * Uses PESSIMISTIC_WRITE lock to prevent race conditions where multiple workers
     * try to claim the same task simultaneously.
     * The lock is held until the transaction commits, ensuring only one worker can claim each task.
     * Orders by createdAt to implement FIFO (First In, First Out) task processing.
     *
     * @return Optional containing the locked task, or empty if no PENDING tasks available
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.status = dev.jjcoll.distributedtaskviz.model.TaskStatus.PENDING " +
           "AND t.assignedWorker IS NULL ORDER BY t.createdAt ASC LIMIT 1")
    Optional<Task> findFirstPendingTaskForLocking();
}
