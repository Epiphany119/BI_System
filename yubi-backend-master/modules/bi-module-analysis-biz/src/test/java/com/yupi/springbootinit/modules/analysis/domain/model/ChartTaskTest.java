package com.yupi.springbootinit.modules.analysis.domain.model;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskMode;
import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** ChartTask 聚合根的重连计数和同步转异步行为测试。 */
class ChartTaskTest {
    /** 验证同一个任务重连三次后可以转入异步等待，任务编号保持不变。 */
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

    /** 验证异步任务不能再次执行同步转异步回退。 */
    @Test
    void asyncTaskCannotFallbackAgain() {
        ChartTask task = new ChartTask(1L, "AT002", 100L, ChartTaskMode.ASYNC, ChartTaskStatus.RUNNING);
        assertThrows(IllegalStateException.class, task::fallbackToAsync);
    }
}
