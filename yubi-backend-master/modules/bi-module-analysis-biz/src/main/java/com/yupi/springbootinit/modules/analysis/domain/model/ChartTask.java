package com.yupi.springbootinit.modules.analysis.domain.model;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskMode;
import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskStatus;
import com.yupi.springbootinit.modules.analysis.domain.service.ChartTaskStateMachine;

import java.time.LocalDateTime;
import java.util.Objects;

/** Analysis task aggregate root. Persistence annotations must stay outside the domain model. */
public class ChartTask {
    private final Long id;
    private final String taskNo;
    private final Long userId;
    private ChartTaskMode mode;
    private ChartTaskStatus status;
    private String executionStage;
    private int retryCount;
    private int reconnectCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public ChartTask(Long id, String taskNo, Long userId, ChartTaskMode mode, ChartTaskStatus status) {
        this.id = id;
        this.taskNo = Objects.requireNonNull(taskNo, "taskNo");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.status = Objects.requireNonNull(status, "status");
    }

    public void transitionTo(ChartTaskStatus target) {
        ChartTaskStateMachine.requireTransition(status, target);
        status = target;
        if (target == ChartTaskStatus.RUNNING && startedAt == null) startedAt = LocalDateTime.now();
        if (target == ChartTaskStatus.SUCCEEDED || target == ChartTaskStatus.FAILED || target == ChartTaskStatus.CANCELED) {
            finishedAt = LocalDateTime.now();
        }
    }

    public void fallbackToAsync() {
        if (mode != ChartTaskMode.SYNC || status != ChartTaskStatus.RUNNING) {
            throw new IllegalStateException("Only a running synchronous task can fallback to async");
        }
        transitionTo(ChartTaskStatus.WAITING);
        mode = ChartTaskMode.ASYNC;
        executionStage = null;
    }

    public void recordReconnect() { reconnectCount++; }
    public void recordAsyncRetry() { retryCount++; }

    public Long getId() { return id; }
    public String getTaskNo() { return taskNo; }
    public Long getUserId() { return userId; }
    public ChartTaskMode getMode() { return mode; }
    public ChartTaskStatus getStatus() { return status; }
    public String getExecutionStage() { return executionStage; }
    public int getRetryCount() { return retryCount; }
    public int getReconnectCount() { return reconnectCount; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}
