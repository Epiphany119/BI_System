package com.yupi.bi.analysis.domain.service;

import com.yupi.bi.analysis.domain.enums.ChartTaskMode;
import com.yupi.bi.analysis.domain.enums.ChartTaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChartTaskDomainTest {
    @Test
    void stateMachineSupportsInitialRoutingAndFallback() {
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.RUNNING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.WAITING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.RUNNING, ChartTaskStatus.WAITING));
        assertFalse(ChartTaskStateMachine.canTransition(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.RUNNING));
    }

    @Test
    void routerUsesTwoMbBoundaryAndFiveMbLimit() {
        long mb = 1024 * 1024;
        assertEquals(ChartTaskMode.SYNC, ChartTaskRouter.route(2 * mb, 2 * mb, 5 * mb));
        assertEquals(ChartTaskMode.ASYNC, ChartTaskRouter.route(2 * mb + 1, 2 * mb, 5 * mb));
        assertThrows(IllegalArgumentException.class, () -> ChartTaskRouter.route(5 * mb + 1, 2 * mb, 5 * mb));
    }
}
