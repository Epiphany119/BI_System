package com.yupi.springbootinit.modules.analysis.domain.model;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskMode;
import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskStatus;

import java.time.LocalDateTime;

public record ChartTaskEvent(
        Long taskId,
        String taskNo,
        String eventType,
        ChartTaskStatus fromStatus,
        ChartTaskStatus toStatus,
        ChartTaskMode executionMode,
        int attempt,
        int reconnectCount,
        String traceId,
        LocalDateTime occurredAt
) { }
