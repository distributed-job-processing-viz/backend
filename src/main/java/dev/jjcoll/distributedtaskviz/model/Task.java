package dev.jjcoll.distributedtaskviz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Task entity representing a distributed task in the system.
 * The @Entity annotation marks this class as a JPA entity, meaning it will be
 * mapped to a database table. By default, the table name will be "task".
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    /**
     * Primary key for the Task entity.
     * @Id marks this field as the primary key in the database.
     * @GeneratedValue tells JPA to automatically generate the ID value.
     * GenerationType.IDENTITY means the database will auto-increment this value.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name/description of the task.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Current status of the task (e.g., "PENDING", "RUNNING", "COMPLETED", "FAILED").
     * Make sure to use EumType. STRING to store the actual value of the enum to the database
     * else we store the ordinal value of the enum, and if we reorder or add values in the middle
     * data becomes corrupted
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Complexity complexity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    /**
     * Worker assigned to process this task (nullable until claimed by a worker).
     * Many tasks can be assigned to one worker.
     * FetchType.LAZY avoids loading worker data unless explicitly needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_worker_id", nullable = true)
    private Worker assignedWorker;

    /**
     * Timestamp when processing started (when task status changed to PROCESSING).
     * Useful for monitoring task processing duration.
     */
    @Column(nullable = true)
    private LocalDateTime processingStartedAt;
}
