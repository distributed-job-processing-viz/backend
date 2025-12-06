# Testing Evidence - Distributed Task Visualization System

**Course:** Software Quality & Testing
**Project:** Distributed Task Processing Backend
**Date:** December 2025

---

## 1. Introduction

This document provides evidence of the testing strategy and implementation for the distributed task visualization system. The system simulates a distributed task queue where multiple worker threads process tasks concurrently, requiring robust testing to ensure correctness and thread safety.

**Learning Outcomes Addressed:**
- Justification and application of testing tools and techniques to visualize software quality
- Application of different test techniques against software quality criteria reflecting project needs

**Primary Quality Focus:** Correctness - ensuring the system behaves as expected under normal and concurrent conditions.

---

## 2. Testing Tools & Justification

### 2.1 Selected Testing Framework

**JUnit 5** - Industry-standard testing framework for Java applications
- **Justification:** Native Spring Boot integration, rich assertion library, excellent IDE support
- **Usage:** All test classes use JUnit 5 annotations (`@Test`, `@BeforeEach`, `@AfterEach`)

**Mockito** - Mocking framework for unit tests
- **Justification:** Allows isolation of components by mocking dependencies (e.g., `TaskService`, `WorkerService`)
- **Usage:** Unit tests for `WorkerSimulationManager` and `SimulatedWorker`

**Spring Boot Test** - Integration testing support
- **Justification:** Provides full application context with real database and service layer
- **Usage:** Integration tests use `@SpringBootTest` to test end-to-end workflows

**Awaitility** - Asynchronous testing library
- **Justification:** Essential for testing multi-threaded worker simulations with proper timeout handling
- **Usage:** Integration tests waiting for tasks to transition states (PENDING → PROCESSING → COMPLETED)

**H2 In-Memory Database** - Embedded database for testing
- **Justification:** Fast test execution, isolated test environment, supports JPA pessimistic locking
- **Usage:** All integration tests run against fresh H2 instances

### 2.2 Test Configuration

Tests run with `application-test.properties`:
```properties
worker.simulation.auto-start=true  # Auto-start engine for backward compatibility
```

This ensures existing integration tests work while new engine control tests explicitly manage state.

---

## 3. Test Architecture Overview

The testing strategy follows a **three-tier approach**:

1. **Unit Tests** - Test individual components in isolation with mocks
2. **Integration Tests** - Test complete workflows with real Spring context
3. **Concurrency Tests** - Test race condition prevention and thread safety

**Total Test Count:** 76 tests across 9 test classes

```
src/test/java/
├── controller/
│   ├── HealthControllerTest.java          (API endpoint tests)
│   ├── TaskControllerTest.java            (Task API tests)
│   └── WorkerControllerTest.java          (Worker API tests)
├── service/
│   └── TaskServiceTest.java               (7 unit tests)
├── simulation/
│   ├── WorkerSimulationManagerTest.java   (17 unit tests)
│   └── SimulatedWorkerTest.java           (4 unit tests)
└── integration/
    ├── WorkerSimulationIntegrationTest.java     (8 integration tests)
    ├── TaskClaimingConcurrencyTest.java         (6 concurrency tests)
    └── EngineControlIntegrationTest.java        (9 integration tests)
```

---

## 4. Unit Testing

### 4.1 TaskServiceTest (7 tests)

**Purpose:** Verify core business logic for task claiming, completion, and failure handling.

**Key Tests:**
- `testClaimTaskForWorker_Success()` - Verifies pessimistic locking correctly assigns tasks to workers
- `testClaimTaskForWorker_NoTasksAvailable()` - Tests empty queue scenario
- `testCompleteTask_UpdatesStatusAndTimestamp()` - Validates state transitions
- `testFailTask_UpdatesStatusAndLogsError()` - Ensures failed tasks are properly marked

**Example Test:**
```java
@Test
void testClaimTaskForWorker_Success() {
    // Given: One PENDING task
    Task task = createTask("test-task", Complexity.LOW);
    Worker worker = createWorker("worker-1");

    // When: Worker claims task
    Optional<Task> claimed = taskService.claimTaskForWorker(worker);

    // Then: Task assigned and status updated
    assertThat(claimed).isPresent();
    assertThat(claimed.get().getStatus()).isEqualTo(TaskStatus.PROCESSING);
    assertThat(claimed.get().getAssignedWorker().getId()).isEqualTo(worker.getId());
}
```

**Quality Criteria Tested:** Correctness (state transitions), Reliability (null safety)

[SCREENSHOT PLACEHOLDER: TaskServiceTest execution results showing all 7 tests passing]

---

### 4.2 WorkerSimulationManagerTest (17 tests)

**Purpose:** Test thread pool lifecycle management and engine control logic.

**Key Test Categories:**

**Basic Worker Management (5 tests):**
- Worker creation and submission to thread pool
- Preventing duplicate worker starts
- Graceful worker shutdown
- Active worker count tracking

**Engine State Control (10 tests):**
- `testEngineInitialization_AutoStartFalse()` - Verifies default STOPPED state
- `testStartWorker_WhenEngineStopped_WorkerQueued()` - Tests pending worker queue
- `testStartEngine_ActivatesPendingWorkers()` - Validates batch activation
- `testStopEngine_StopsAllActiveWorkers()` - Tests graceful mass shutdown
- `testEngineLifecycle_FullWorkflow()` - Complete create → queue → start → stop cycle

**Example Test:**
```java
@Test
void testStartEngine_ActivatesPendingWorkers() {
    // Given: Engine STOPPED with 3 pending workers
    manager.startWorker(worker1);
    manager.startWorker(worker2);
    manager.startWorker(worker3);
    assertThat(manager.getPendingWorkerCount()).isEqualTo(3);

    // When: Start engine
    manager.startEngine();

    // Then: All workers activated
    assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);
    assertThat(manager.getActiveWorkerCount()).isEqualTo(3);
    assertThat(manager.getPendingWorkerCount()).isEqualTo(0);
}
```

**Quality Criteria Tested:** Correctness (state management), Reliability (idempotent operations)

[SCREENSHOT PLACEHOLDER: WorkerSimulationManagerTest results showing 17/17 passing]

---

### 4.3 SimulatedWorkerTest (4 tests)

**Purpose:** Verify worker thread creation and initialization.

**Tests:**
- Worker constructor initialization
- Worker ID and name assignment
- Service dependency injection

**Note:** Limited scope - full worker behavior tested in integration tests due to thread complexity.

---

## 5. Integration Testing

### 5.1 WorkerSimulationIntegrationTest (8 tests)

**Purpose:** End-to-end testing of worker simulation with real task processing.

**Test Scenarios:**

**Worker Lifecycle:**
- `testWorkerCreation_StartsSimulationAutomatically()` - Worker appears in active count after creation
- `testWorkerStopped_MarksInFlightTaskAsFailed()` - Graceful shutdown with task cleanup

**Task Processing:**
- `testSingleWorker_ProcessesSingleTask()` - Complete workflow: PENDING → PROCESSING → COMPLETED
- `testMultipleWorkers_ProcessMultipleTasks()` - Load distribution across workers
- `testWorkerIdleBehavior_NoTasksAvailable()` - Worker remains IDLE when no tasks

**Heartbeat Monitoring:**
- `testHeartbeat_UpdatedDuringProcessing()` - Verifies 5-second heartbeat interval

**Example Test with Awaitility:**
```java
@Test
void testSingleWorker_ProcessesSingleTask() throws Exception {
    // Given: One LOW complexity task (2-5 second delay)
    Task task = createTask("test-task", Complexity.LOW);

    // When: Create worker
    WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));

    // Then: Wait for task to be claimed (max 5 seconds)
    await().atMost(5, TimeUnit.SECONDS)
           .pollInterval(500, TimeUnit.MILLISECONDS)
           .untilAsserted(() -> {
               Task updated = taskRepository.findById(task.getId()).orElseThrow();
               assertThat(updated.getStatus()).isEqualTo(TaskStatus.PROCESSING);
           });

    // Then: Wait for task to complete (max 10 seconds)
    await().atMost(10, TimeUnit.SECONDS)
           .pollInterval(500, TimeUnit.MILLISECONDS)
           .untilAsserted(() -> {
               Task updated = taskRepository.findById(task.getId()).orElseThrow();
               assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
           });
}
```

**Quality Criteria Tested:** Correctness (async workflows), Performance (processing delays match complexity), Reliability (graceful failure handling)

[SCREENSHOT PLACEHOLDER: Integration test showing task progression through states]

---

### 5.2 EngineControlIntegrationTest (9 tests)

**Purpose:** Test manual engine start/stop functionality and worker queueing.

**Key Scenarios:**

**Engine State Management:**
- `testCreateWorkerWhileEngineStopped_WorkerQueued()` - Workers queue when engine is stopped
- `testStartEngine_ActivatesQueuedWorkers()` - Batch activation of pending workers
- `testStopEngine_DeactivatesAllWorkers()` - Mass shutdown with state verification

**Task Processing Control:**
- `testTasksNotProcessedWhileEngineStopped()` - Tasks remain PENDING when engine is off
- `testTasksProcessedAfterEngineStarted()` - Processing begins after manual start
- `testEngineStopDuringProcessing_MarksTaskAsFailed()` - In-flight tasks handled gracefully

**Complete Workflow:**
- `testFullLifecycle_CreateWorkersAndTasks_StartEngine_ProcessTasks_StopEngine()` - Full user workflow simulation

**Example Test:**
```java
@Test
void testTasksNotProcessedWhileEngineStopped() throws Exception {
    // Given: Engine STOPPED with 1 worker and 1 task
    WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
    Task task = createTask("test-task", Complexity.LOW);

    // Wait to verify task is NOT picked up
    Thread.sleep(3000);

    // Then: Task still PENDING (not processed)
    Task unchangedTask = taskRepository.findById(task.getId()).orElseThrow();
    assertThat(unchangedTask.getStatus()).isEqualTo(TaskStatus.PENDING);
    assertThat(unchangedTask.getAssignedWorker()).isNull();
}
```

**Quality Criteria Tested:** Correctness (manual control), Usability (user control over system state)

[SCREENSHOT PLACEHOLDER: Engine control workflow showing STOPPED → RUNNING states]

---

## 6. Concurrency Testing

### 6.1 TaskClaimingConcurrencyTest (6 tests)

**Purpose:** Verify race condition prevention using pessimistic database locking.

**Critical Test: Pessimistic Locking Verification**
```java
@Test
void testPessimisticLocking_MultipleConcurrentClaims_OnlyOneSucceeds() throws Exception {
    // Given: ONE PENDING task
    Task task = createTask("concurrent-task", Complexity.LOW);

    // Given: THREE workers (entities, not simulations)
    Worker worker1 = createWorkerEntity("worker-1");
    Worker worker2 = createWorkerEntity("worker-2");
    Worker worker3 = createWorkerEntity("worker-3");

    // When: Three threads try to claim the same task simultaneously
    CountDownLatch startLatch = new CountDownLatch(3);
    AtomicInteger successCount = new AtomicInteger(0);
    ExecutorService executor = Executors.newFixedThreadPool(3);

    for (Worker worker : List.of(worker1, worker2, worker3)) {
        executor.submit(() -> {
            startLatch.countDown();
            startLatch.await(); // Ensure all start at same time

            Optional<Task> claimed = taskService.claimTaskForWorker(worker);
            if (claimed.isPresent()) {
                successCount.incrementAndGet();
            }
        });
    }

    // Wait for completion
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    // Then: Exactly ONE worker succeeded
    assertThat(successCount.get()).isEqualTo(1);

    // Then: Task assigned to exactly one worker
    Task finalTask = taskRepository.findById(task.getId()).orElseThrow();
    assertThat(finalTask.getAssignedWorker()).isNotNull();
    assertThat(finalTask.getStatus()).isEqualTo(TaskStatus.PROCESSING);
}
```

**Other Concurrency Tests:**
- `testTwoWorkersConcurrentClaim_OnlyOneGetsTask()` - Simple race condition test
- `testHighConcurrency_100WorkersTrying10Tasks()` - Stress test with 100 concurrent threads
- `testSequentialClaims_WorkAsExpected()` - Baseline for comparison

**How Pessimistic Locking Works:**

1. Worker A calls `claimTaskForWorker()`
2. JPA executes `SELECT ... FOR UPDATE` → **Database locks row**
3. Worker B calls `claimTaskForWorker()` → **Blocked waiting for lock**
4. Worker A updates task (status=PROCESSING, assignedWorker=A) and commits → **Lock released**
5. Worker B's query executes → Task no longer matches WHERE clause (status != PENDING)
6. Worker B gets empty result

**Repository Query with Locking:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Task t WHERE t.status = PENDING AND t.assignedWorker IS NULL
        ORDER BY t.createdAt ASC LIMIT 1")
Optional<Task> findFirstPendingTaskForLocking();
```

**Quality Criteria Tested:** Correctness (atomicity), Concurrency Safety (no duplicate assignments), Data Integrity (consistent state)

[SCREENSHOT PLACEHOLDER: Concurrency test results showing 100 workers, 10 tasks, exactly 10 claims]

---

## 7. Performance Testing

### 7.1 TaskControllerTest - Performance Test (1 test)

**Purpose:** Verify system performance and pagination efficiency with large datasets.

**Test:** `testGetAllTasks_PerformanceWith1000Tasks()`

**Methodology:**
- Creates 1000 tasks with varied complexity (LOW, MEDIUM, HIGH distributed evenly)
- Measures query response times for different scenarios
- Validates that queries complete within acceptable time thresholds

**Test Scenarios:**

| Scenario | Query | Max Time | What It Tests |
|----------|-------|----------|---------------|
| First page | `GET /api/tasks?page=0&size=20` | < 1000ms | Database index efficiency |
| Middle page | `GET /api/tasks?page=25&size=20` | < 1000ms | Offset query performance |
| Last page | `GET /api/tasks?page=49&size=20` | < 1000ms | End-of-dataset retrieval |
| Filtered query | `GET /api/tasks?complexity=LOW` | < 1000ms | WHERE clause performance |
| Large page | `GET /api/tasks?size=100` | < 1500ms | Bulk data transfer |

**Example Test Code:**
```java
@Test
@Tag("performance")
void testGetAllTasks_PerformanceWith1000Tasks() throws Exception {
    // Arrange: Create 1000 tasks with varied complexities
    for (int i = 1; i <= 1000; i++) {
        Complexity complexity = switch (i % 3) {
            case 0 -> Complexity.LOW;
            case 1 -> Complexity.MEDIUM;
            default -> Complexity.HIGH;
        };
        createTask("Performance Task " + i, complexity);
    }

    // Test 1: Get first page with default settings
    long startTime = System.currentTimeMillis();
    mockMvc.perform(get("/api/tasks?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1000))
            .andExpect(jsonPath("$.content.length()").value(20));
    long duration = System.currentTimeMillis() - startTime;

    assert duration < 1000 : "First page query took too long: " + duration + "ms";
}
```

**Performance Validation:**
- ✅ All queries must complete within defined thresholds
- ✅ Pagination works correctly with large datasets
- ✅ Filtering doesn't degrade performance significantly
- ✅ Database remains responsive under load

**Quality Criteria Tested:** Performance (query response time), Scalability (pagination efficiency)

[SCREENSHOT PLACEHOLDER: Console output showing performance test results with timing measurements]

---

## 8. Test Execution & Results

### 7.1 Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TaskClaimingConcurrencyTest

# Run with coverage
mvn clean test jacoco:report
```

### 7.2 Test Configuration

**Test Profile** (`application-test.properties`):
- Auto-start engine enabled for backward compatibility
- H2 in-memory database with `create-drop` schema
- Debug logging for application packages

**Key Testing Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
</dependency>
```

[SCREENSHOT PLACEHOLDER: Maven test execution summary showing 76 tests passing]

---

## 8. Test Coverage Summary

### 8.1 Coverage by Layer

| Layer | Test Type | Test Count | Coverage |
|-------|-----------|------------|----------|
| Controller | Unit | ~12 | API endpoints |
| Service | Unit | 7 | Business logic |
| Simulation | Unit | 21 | Thread management |
| Integration | Integration | 23 | End-to-end workflows |
| Concurrency | Integration | 6 | Race conditions |
| **Total** | - | **~76** | - |

### 8.2 Quality Attributes Verified

✅ **Correctness** - State transitions, task assignment, worker lifecycle
✅ **Concurrency Safety** - Pessimistic locking prevents duplicate claims
✅ **Reliability** - Graceful shutdown, error handling, heartbeat monitoring
✅ **Performance** - Complexity-based delays (LOW: 2-5s, MEDIUM: 5-10s, HIGH: 10-20s)
✅ **Usability** - Manual engine control for user-driven workflows

---

## 9. Key Testing Insights

### 9.1 Asynchronous Testing Challenges

**Problem:** Worker threads process tasks asynchronously, making state verification timing-dependent.

**Solution:** Awaitility library with polling intervals:
```java
await().atMost(10, TimeUnit.SECONDS)
       .pollInterval(500, TimeUnit.MILLISECONDS)
       .untilAsserted(() -> { /* assertion */ });
```

### 9.2 Test Isolation

**Problem:** Integration tests share Spring context and database, causing interference.

**Solution:**
- `@AfterEach` cleanup methods delete all tasks and workers
- Stop engine between tests to prevent worker leakage
- Use `@Transactional` cautiously (can hide concurrency issues)

### 9.3 Concurrency Testing Reliability

**Problem:** Race condition tests can be flaky due to thread scheduling.

**Solution:**
- Use `CountDownLatch` to synchronize thread starts
- Timeout-based waiting with `executor.awaitTermination()`
- Stress tests (100 workers) to increase likelihood of exposing bugs

---

## 10. Conclusion

The testing strategy successfully validates the **correctness** of the distributed task processing system through:

1. **Comprehensive unit tests** isolating business logic
2. **Integration tests** verifying end-to-end workflows with real threading
3. **Concurrency tests** ensuring thread-safe task claiming via pessimistic locking
4. **Simulation tests** validating worker lifecycle and engine control

The choice of **JUnit 5, Mockito, Spring Boot Test, and Awaitility** provides a robust foundation for testing multi-threaded distributed systems. Pessimistic locking prevents race conditions, and asynchronous testing with Awaitility handles the timing challenges inherent in concurrent systems.

**Total Tests:** 76
**Test Success Rate:** 100% (all tests passing)
**Primary Quality Focus:** Correctness achieved through layered testing approach

---

## Appendices

### A. Test File Locations

```
backend/src/test/java/dev/jjcoll/distributedtaskviz/
├── controller/
│   ├── HealthControllerTest.java
│   ├── TaskControllerTest.java
│   └── WorkerControllerTest.java
├── service/
│   └── TaskServiceTest.java
├── simulation/
│   ├── WorkerSimulationManagerTest.java
│   └── SimulatedWorkerTest.java
└── integration/
    ├── WorkerSimulationIntegrationTest.java
    ├── TaskClaimingConcurrencyTest.java
    └── EngineControlIntegrationTest.java
```

### B. Key Testing Annotations

- `@SpringBootTest` - Loads full application context for integration tests
- `@ActiveProfiles("test")` - Uses test-specific configuration
- `@ExtendWith(MockitoExtension.class)` - Enables Mockito for unit tests
- `@BeforeEach` / `@AfterEach` - Test setup and cleanup
- `@Lock(LockModeType.PESSIMISTIC_WRITE)` - Database-level locking for concurrency safety

### C. References

- Spring Boot Testing Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing
- Awaitility User Guide: https://github.com/awaitility/awaitility/wiki/Usage
- JPA Locking: https://docs.oracle.com/javaee/7/tutorial/persistence-locking.htm