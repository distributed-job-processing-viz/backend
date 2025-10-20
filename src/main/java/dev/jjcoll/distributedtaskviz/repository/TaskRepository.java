package dev.jjcoll.distributedtaskviz.repository;


import dev.jjcoll.distributedtaskviz.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

    public interface TaskRepository extends JpaRepository<Task, Long> {
}
