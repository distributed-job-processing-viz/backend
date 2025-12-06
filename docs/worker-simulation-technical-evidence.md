# Worker Simulation System - Technical Evidence

**Student:** [Your Name]
**Project:** Distributed Task Visualization System
**Date:** December 2025

---

## 1. Introduction

This document provides evidence of how I designed and implemented a worker simulation system for a distributed task processing application. The system allows users to create virtual workers that automatically claim and process tasks from a shared queue, simulating a real-world distributed computing environment.

The project demonstrates full-stack development capabilities, user-centered design principles, and justified architectural decisions for a complex concurrent system.

---

## 2. System Overview

### 2.1 What the System Does

The worker simulation system consists of three main components working together:

- **Backend API (Spring Boot)**: RESTful endpoints for creating workers and tasks, managing engine state
- **Worker Simulation Engine**: Background threads that simulate worker behavior (claiming tasks, processing, updating status)
- **Database Layer (JPA/Hibernate)**: Persistent storage with concurrency control mechanisms

Users interact with the system through a web interface where they can:
1. Create workers and tasks
2. Start/pause/resume/stop the processing engine
3. Monitor workers in real-time (heartbeat updates every 5 seconds)
4. View task processing progress and completion status

---

## 3. Key Architectural Decisions

### 3.1 Thread Pool vs Dedicated Threads

**Decision**: Use a shared `ThreadPoolExecutor` rather than creating one thread per worker.

**Why**:
- **Resource efficiency**: Creating 100 workers won't create 100 OS threads
- **Bounded resources**: The pool has configurable limits (core: 5, max: 10, queue: 100)
- **Backpressure handling**: When the pool is full, the `CallerRunsPolicy` naturally throttles the system instead of crashing

**User Impact**: The system remains responsive even if users create many workers. There's no risk of exhausting system resources.

### 3.2 Pessimistic Locking for Race Condition Prevention

**Decision**: Use database-level `PESSIMISTIC_WRITE` locking when workers claim tasks.

**The Problem**:
Without locking, two workers could read the same pending task simultaneously and both try to process it, causing data corruption.

**The Solution** (`TaskRepository.java:17`):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Task t WHERE t.status = 'PENDING'
       AND t.assignedWorker IS NULL
       ORDER BY t.createdAt ASC")
Optional<Task> findFirstPendingTaskForLocking();
```

**How It Works**:
1. Worker A calls this query → Database locks the row with `SELECT ... FOR UPDATE`
2. Worker B tries to query → B waits because the row is locked
3. Worker A updates the task (status=PROCESSING, assignedWorker=A) and commits
4. Worker B's query runs but the task no longer matches the WHERE clause
5. Worker B gets the next available task or empty result

**Why This Approach**: Database locks are more reliable than application-level synchronization in distributed systems. Even if we deploy multiple application instances in the future, the database guarantees only one worker gets each task.

**Alternative Considered**: Optimistic locking with version numbers. We rejected this because it would require retry logic and could cause workers to "waste" cycles on failed claims.

### 3.3 Pause vs Stop: The Trade-off

**Decision**: Implement pause using a polling check rather than thread interruption.

**How Pause Works** (`SimulatedWorker.java:60-64`):
```java
if (simulationManager.getEngineState() == EngineState.PAUSED) {
    workerService.updateWorkerStatus(worker.getId(), WorkerStatus.IDLE);
    Thread.sleep(100); // Brief sleep while paused
    continue;
}
```

When paused:
- Worker threads remain alive in the pool
- Each worker checks the engine state every 100ms
- Workers skip task claiming and just sleep
- Heartbeats continue updating

**Why This Approach**:
This is admittedly **not the most efficient** solution. Workers continue running and checking state even though they're doing nothing productive. A more efficient approach would be:

1. Maintain a `Map<Long, Worker>` of all worker entities
2. When pause is called, interrupt all threads (like stop does)
3. When resume is called, re-submit all workers to the thread pool

**Why We Chose the Simpler Approach**:
- **Less state management**: We don't need to track which workers were paused vs stopped
- **No worker ID tracking**: The efficient approach requires maintaining a separate collection of worker IDs or entities
- **Fewer edge cases**: What happens if a worker is deleted while paused? The complex approach needs extra logic
- **Acceptable overhead**: The 100ms sleep is cheap, and pause is typically temporary

**User-Centered Justification**: Users expect pause/resume to be instant and seamless. The polling approach guarantees workers can resume immediately without needing to reconstruct thread state. The performance overhead is negligible for a simulation/teaching tool.

---

## 4. User-Centered Design Considerations

### 4.1 Manual Engine Control

**Design Choice**: The engine starts in STOPPED state by default. Workers created before the engine starts are queued as "pending."

**Why**:
- **User control**: Users can set up their scenario (create tasks, create workers) before starting the simulation
- **Educational value**: Students can see the difference between "created" workers and "active" workers
- **Predictability**: The system doesn't start processing immediately, which could be confusing

**Implementation** (`WorkerSimulationManager.java:104-109`):
When a worker is created while the engine is stopped, it's added to a `pendingWorkers` list. When `startEngine()` is called, all pending workers are activated at once.

### 4.2 Heartbeat Updates

**Design Choice**: Workers update their heartbeat timestamp every 5 seconds (`SimulatedWorker.java:31`).

**Why**:
- **Visual feedback**: Users can see workers are alive and functioning
- **Real-world simulation**: Actual distributed systems use heartbeats to detect failed nodes
- **Debugging aid**: If a worker stops updating, we know it's stuck or crashed

**User Impact**: The UI can display "last seen" timestamps, giving users confidence the system is working correctly.

### 4.3 Graceful Shutdown

**Design Choice**: When the application shuts down, the system waits up to 30 seconds for in-flight tasks to complete (`WorkerSimulationManager.java:169-191`).

**Why**:
- **Data integrity**: Tasks aren't left in a "PROCESSING" state forever
- **Predictable behavior**: Users see tasks either complete or fail, never "stuck"
- **Real-world practice**: Production systems should always shut down gracefully

---

## 5. How the System Works (End-to-End Flow)

### 5.1 Creating and Starting Workers

1. **User** sends `POST /api/workers` with worker name
2. **WorkerService** creates Worker entity in database (status=STOPPED)
3. **WorkerService** calls `simulationManager.startWorker(worker)`
4. **If engine is STOPPED**: Worker is added to `pendingWorkers` list
5. **User** sends `POST /api/engine/start`
6. **WorkerSimulationManager** changes state to RUNNING, submits all pending workers to thread pool
7. **Each SimulatedWorker thread** begins its processing loop

### 5.2 Task Processing Loop

Each `SimulatedWorker` runs this loop until interrupted:

1. **Check interruption**: Exit if thread was cancelled
2. **Update heartbeat**: If 5 seconds elapsed, update timestamp in database
3. **Check engine state**: If PAUSED, sleep 100ms and restart loop
4. **Claim task**: Call `taskService.claimTaskForWorker(worker)` with pessimistic lock
5. **If task found**:
   - Update worker status to PROCESSING
   - Sleep for random duration based on task complexity (Low: 2-5s, Medium: 5-10s, High: 10-20s)
   - Mark task as COMPLETED
6. **If no task found**:
   - Update worker status to IDLE
   - Sleep 1 second before retrying

### 5.3 Pausing the Engine

1. **User** sends `POST /api/engine/pause`
2. **WorkerSimulationManager** sets `engineState = PAUSED`
3. **All worker threads** detect pause on next loop iteration (within 100ms if idle, or during task processing)
4. **Workers** set status to IDLE and sleep in 100ms increments
5. **User** sends `POST /api/engine/resume`
6. **Workers** immediately resume claiming tasks

**Why this works**: The `engineState` field is marked `volatile` (`WorkerSimulationManager.java:36`), ensuring changes are visible across all threads immediately.

---

## 6. Validation and Testing

### 6.1 Race Condition Test

**Test**: Created 2 workers, 1 task, started engine simultaneously.

**Expected**: Only one worker processes the task.

**Result**: Worker A claimed task, Worker B received empty result. Database enforced mutual exclusion.

### 6.2 Graceful Shutdown Test

**Test**: Started worker processing a 15-second task, sent `DELETE /api/workers/{id}` after 5 seconds.

**Expected**: Task marked as FAILED, worker stops.

**Result**: Worker thread caught `InterruptedException`, marked task as FAILED, exited cleanly (`SimulatedWorker.java:77-79, 115-120`).

### 6.3 Heartbeat Verification

**Test**: Monitored worker table in database while worker was idle.

**Expected**: `lastHeartbeat` timestamp updates every ~5 seconds.

**Result**: Confirmed updates occurring consistently (`SimulatedWorker.java:167-179`).

---

## 7. Reflection on Architectural Choices

### 7.1 What Worked Well

- **Pessimistic locking**: Zero race conditions observed, even under high concurrency
- **Thread pool**: System remained stable with 20+ simultaneous workers
- **Graceful shutdown**: No orphaned tasks in PROCESSING state after testing application restarts

### 7.2 Known Limitations

- **Pause efficiency**: Workers consume CPU polling every 100ms when paused (acceptable for simulation, not for production)
- **Single instance only**: The system assumes one application instance (no true distributed coordination)
- **No task priorities**: Tasks are processed FIFO only

### 7.3 Future Improvements

If I were to extend this system, I would consider:

1. **Event-based pause/resume**: Use `CountDownLatch` or similar to block threads efficiently when paused
2. **Task priorities**: Add priority field and sort in query (`ORDER BY priority DESC, createdAt ASC`)
3. **Distributed coordination**: Use Redis or database advisory locks to support multiple application instances
4. **Worker affinity**: Allow tasks to specify required worker capabilities

---

## 8. Conclusion

This worker simulation system demonstrates my ability to:

- **Design concurrent systems** with proper synchronization mechanisms (pessimistic locking, thread pools)
- **Make justified architectural trade-offs** (polling-based pause for simplicity vs efficiency)
- **Apply user-centered design** (manual engine control, heartbeat visibility, graceful shutdown)
- **Develop full-stack applications** (REST API, JPA persistence, background processing)

The system successfully simulates a distributed task queue while maintaining data integrity and providing users with full control over the simulation lifecycle. All architectural decisions were made with both technical correctness and user experience in mind.
