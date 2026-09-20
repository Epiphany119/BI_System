package com.yupi.springbootinit.modules.analysis.domain.service;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChartTaskRouterTest {
    private static final long MB = 1024 * 1024;

    @Test
    void routesAtTwoMbBoundaryToSync() {
        assertEquals(ChartTaskMode.SYNC, ChartTaskRouter.route(2 * MB, 2 * MB, 5 * MB));
        assertEquals(ChartTaskMode.ASYNC, ChartTaskRouter.route(2 * MB + 1, 2 * MB, 5 * MB));
    }

    @Test
    void rejectsFilesAboveFiveMb() {
        assertThrows(IllegalArgumentException.class,
                () -> ChartTaskRouter.route(5 * MB + 1, 2 * MB, 5 * MB));
    }
}
