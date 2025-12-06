package dev.jjcoll.distributedtaskviz.model;

/**
 * Represents the state of the task processing engine.
 * The engine controls whether workers actively process tasks.
 */
public enum EngineState {
    /**
     * Engine is stopped. Workers are terminated and removed.
     * New workers can be created but won't process until engine starts.
     */
    STOPPED,

    /**
     * Engine is paused. Worker threads remain alive but idle.
     * Tasks are not processed. Workers can be quickly resumed.
     */
    PAUSED,

    /**
     * Engine is running. All workers actively poll for and process tasks.
     */
    RUNNING
}
