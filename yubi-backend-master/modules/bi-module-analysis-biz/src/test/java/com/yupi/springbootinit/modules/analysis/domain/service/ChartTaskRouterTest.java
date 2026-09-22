package com.yupi.springbootinit.modules.analysis.domain.service;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 文件大小路由器测试：验证同步、异步和最大文件限制。 */
class ChartTaskRouterTest {
    private static final long MB = 1024 * 1024;

    /** 验证恰好 2MB 走同步，超过 2MB 走异步。 */
    @Test
    void routesAtTwoMbBoundaryToSync() {
        assertEquals(ChartTaskMode.SYNC, ChartTaskRouter.route(2 * MB, 2 * MB, 5 * MB));
        assertEquals(ChartTaskMode.ASYNC, ChartTaskRouter.route(2 * MB + 1, 2 * MB, 5 * MB));
    }

    /** 验证超过 5MB 的文件被拒绝。 */
    @Test
    void rejectsFilesAboveFiveMb() {
        assertThrows(IllegalArgumentException.class,
                () -> ChartTaskRouter.route(5 * MB + 1, 2 * MB, 5 * MB));
    }
}
