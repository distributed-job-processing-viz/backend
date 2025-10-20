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
     */
    @Column(nullable = false)
    private String status;

    /**
     * Timestamp when the task was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the task was completed.
     * Can be null if the task hasn't completed yet.
     */
    @Column(nullable = true)
    private LocalDateTime completedAt;
}
