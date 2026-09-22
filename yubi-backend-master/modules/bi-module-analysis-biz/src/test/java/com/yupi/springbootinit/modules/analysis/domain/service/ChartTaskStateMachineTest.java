package com.yupi.springbootinit.modules.analysis.domain.service;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 任务状态机测试，覆盖正常流转、回退重试和终态保护。 */
class ChartTaskStateMachineTest {
    /** 验证新任务可以进入同步运行或异步等待。 */
    @Test
    void allowsCreatedToSyncOrAsyncWaiting() {
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.RUNNING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.WAITING));
    }

    /** 验证同步失败回退和失败任务手动重试都可以回到等待状态。 */
    @Test
    void allowsSyncFallbackAndManualRetry() {
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.RUNNING, ChartTaskStatus.WAITING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.FAILED, ChartTaskStatus.WAITING));
    }

    /** 验证成功和取消等终态不能再次进入运行状态。 */
    @Test
    void terminalStatesCannotRunAgain() {
        assertFalse(ChartTaskStateMachine.canTransition(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.RUNNING));
        assertFalse(ChartTaskStateMachine.canTransition(ChartTaskStatus.CANCELED, ChartTaskStatus.RUNNING));
        assertThrows(ChartTaskTransitionException.class,
                () -> ChartTaskStateMachine.requireTransition(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.RUNNING));
    }
}
