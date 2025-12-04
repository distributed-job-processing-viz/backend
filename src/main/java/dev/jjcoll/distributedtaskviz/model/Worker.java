package dev.jjcoll.distributedtaskviz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Worker entity representing a worker instance in the distributed system.
 * The @Entity annotation marks this class as a JPA entity, meaning it will be
 * mapped to a database table. By default, the table name will be "worker".
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Worker {

    /**
     * Primary key for the Worker entity.
     * @Id marks this field as the primary key in the database.
     * @GeneratedValue tells JPA to automatically generate the ID value.
     * GenerationType.IDENTITY means the database will auto-increment this value.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the worker.
     * Auto-generated if not provided (e.g., "worker-1", "worker-2").
     */
    @Column(nullable = false)
    private String name;

    /**
     * Current status of the worker (e.g., "IDLE", "PROCESSING", "STOPPED").
     * Using EnumType.STRING to store the actual value of the enum to the database
     * rather than the ordinal value, preventing data corruption if enum values are reordered.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerStatus status;

    /**
     * Timestamp when the worker was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last heartbeat received from the worker.
     */
    @Column(nullable = true)
    private LocalDateTime lastHeartbeat;
}