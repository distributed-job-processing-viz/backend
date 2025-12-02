package dev.jjcoll.distributedtaskviz.repository;


import dev.jjcoll.distributedtaskviz.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Making Task repository extend JpaSpecificationExecutor
 * gives us ONE powerful method: taskRepository.findAll(filter, pageAndSort)
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
}
