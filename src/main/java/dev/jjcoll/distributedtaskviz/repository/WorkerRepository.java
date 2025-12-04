package dev.jjcoll.distributedtaskviz.repository;

import dev.jjcoll.distributedtaskviz.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Worker entity.
 * Extends JpaRepository to provide CRUD operations and custom query methods.
 */
@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    /**
     * Count the number of workers with names starting with a specific prefix.
     * This is used for auto-generating worker names.
     *
     * @param namePrefix the prefix to match (e.g., "worker-")
     * @return the count of workers with names starting with the prefix
     */
    long countByNameStartingWith(String namePrefix);
}