package com.yupi.springbootinit.charttask.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChartTaskStateMachineTest {
    @Test
    void allowsCreatedToSyncOrAsyncWaiting() {
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.RUNNING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.CREATED, ChartTaskStatus.WAITING));
    }

    @Test
    void allowsSyncFallbackAndManualRetry() {
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.RUNNING, ChartTaskStatus.WAITING));
        assertTrue(ChartTaskStateMachine.canTransition(ChartTaskStatus.FAILED, ChartTaskStatus.WAITING));
    }

    @Test
    void terminalStatesCannotRunAgain() {
        assertFalse(ChartTaskStateMachine.canTransition(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.RUNNING));
        assertFalse(ChartTaskStateMachine.canTransition(ChartTaskStatus.CANCELED, ChartTaskStatus.RUNNING));
        assertThrows(ChartTaskTransitionException.class,
                () -> ChartTaskStateMachine.requireTransition(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.RUNNING));
    }
}
