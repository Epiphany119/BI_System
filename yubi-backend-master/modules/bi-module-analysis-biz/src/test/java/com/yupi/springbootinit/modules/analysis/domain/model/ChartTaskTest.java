package com.yupi.springbootinit.modules.analysis.domain.model;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskMode;
import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChartTaskTest {
    @Test
    void fallbackReusesTaskAndSwitchesMode() {
        ChartTask task = new ChartTask(1L, "AT001", 100L, ChartTaskMode.SYNC, ChartTaskStatus.RUNNING);
        task.recordReconnect();
        task.recordReconnect();
        task.recordReconnect();

        task.fallbackToAsync();

        assertEquals("AT001", task.getTaskNo());
        assertEquals(ChartTaskMode.ASYNC, task.getMode());
        assertEquals(ChartTaskStatus.WAITING, task.getStatus());
        assertEquals(3, task.getReconnectCount());
    }

    @Test
    void asyncTaskCannotFallbackAgain() {
        ChartTask task = new ChartTask(1L, "AT002", 100L, ChartTaskMode.ASYNC, ChartTaskStatus.RUNNING);
        assertThrows(IllegalStateException.class, task::fallbackToAsync);
    }
}
