package com.yupi.springbootinit.charttask.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** V1.0 task state machine. Routing mode is deliberately separate from status. */
public final class ChartTaskStateMachine {
    private static final Map<ChartTaskStatus, Set<ChartTaskStatus>> TRANSITIONS = Map.of(
            ChartTaskStatus.CREATED, EnumSet.of(ChartTaskStatus.RUNNING, ChartTaskStatus.WAITING, ChartTaskStatus.FAILED),
            ChartTaskStatus.WAITING, EnumSet.of(ChartTaskStatus.RUNNING, ChartTaskStatus.CANCELED, ChartTaskStatus.FAILED),
            ChartTaskStatus.RUNNING, EnumSet.of(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.WAITING,
                    ChartTaskStatus.FAILED, ChartTaskStatus.CANCELED),
            ChartTaskStatus.FAILED, EnumSet.of(ChartTaskStatus.WAITING),
            ChartTaskStatus.SUCCEEDED, EnumSet.noneOf(ChartTaskStatus.class),
            ChartTaskStatus.CANCELED, EnumSet.noneOf(ChartTaskStatus.class)
    );

    private ChartTaskStateMachine() { }

    public static boolean canTransition(ChartTaskStatus from, ChartTaskStatus to) {
        return from != null && to != null && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(ChartTaskStatus from, ChartTaskStatus to) {
        if (!canTransition(from, to)) {
            throw new ChartTaskTransitionException(from, to);
        }
    }
}
