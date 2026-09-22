package com.yupi.bi.analysis.domain.service;

import com.yupi.bi.analysis.domain.enums.ChartTaskMode;
import com.yupi.bi.analysis.domain.enums.ChartTaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 旧任务领域模型的状态流转和执行路由测试。 */
class ChartTaskDomainTest {
    /** 验证初始任务可以进入同步运行或等待，并禁止成功任务重新运行。 */
    @Test
    void stateMachineSupportsInitialRoutingAndFallback() {
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.RUNNING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.WAITING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.RUNNING, ChartTaskStatus.WAITING));
        assertFalse(ChartTaskStateMachine.canTransition(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.RUNNING));
    }

    /** 验证 2MB 同步边界、超过边界转异步以及 5MB 文件上限。 */
    @Test
    void routerUsesTwoMbBoundaryAndFiveMbLimit() {
        long mb = 1024 * 1024;
        assertEquals(ChartTaskMode.SYNC, ChartTaskRouter.route(2 * mb, 2 * mb, 5 * mb));
        assertEquals(ChartTaskMode.ASYNC, ChartTaskRouter.route(2 * mb + 1, 2 * mb, 5 * mb));
        assertThrows(IllegalArgumentException.class, () -> ChartTaskRouter.route(5 * mb + 1, 2 * mb, 5 * mb));
    }
}
