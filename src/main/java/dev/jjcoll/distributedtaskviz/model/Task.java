package dev.jjcoll.distributedtaskviz.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

/**
 * Task entity representing a distributed task in the system.
 * The @Entity annotation marks this class as a JPA entity, meaning it will be
 * mapped to a database table. By default, the table name will be "task".
 */
@Entity
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
     * The 'private' access modifier is correct - it encapsulates the field
     * and forces access through getters/setters (following JavaBean conventions).
     * @Column annotation is optional but allows you to specify database column details.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Current status of the task (e.g., "PENDING", "RUNNING", "COMPLETED", "FAILED").
     * Using String for flexibility, but could also use an Enum for type safety.
     */
    @Column(nullable = false)
    private String status;

    /**
     * Timestamp when the task was created.
     * Using LocalDateTime instead of Date because:
     * - It's part of the modern java.time API (Java 8+)
     * - It's immutable and thread-safe
     * - Better API for date/time manipulation
     * - No timezone confusion (unlike java.util.Date)
     * JPA 2.2+ automatically maps LocalDateTime to TIMESTAMP columns.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the task was completed.
     * Can be null if the task hasn't completed yet.
     */
    @Column(nullable = true)
    private LocalDateTime completedAt;

    // Default constructor required by JPA
    public Task() {
    }

    // Constructor for creating new tasks
    public Task(String name, String status) {
        this.name = name;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    // These are required for JPA and allow controlled access to private fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
