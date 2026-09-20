package com.yupi.bi.analysis.domain.service;

import com.yupi.bi.analysis.domain.enums.ChartTaskStatus;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ChartTaskStateMachine {
    private static final Map<ChartTaskStatus, Set<ChartTaskStatus>> TRANSITIONS = Map.of(
            ChartTaskStatus.CREATED, EnumSet.of(ChartTaskStatus.RUNNING, ChartTaskStatus.WAITING, ChartTaskStatus.FAILED),
            ChartTaskStatus.WAITING, EnumSet.of(ChartTaskStatus.RUNNING, ChartTaskStatus.CANCELED, ChartTaskStatus.FAILED),
            ChartTaskStatus.RUNNING, EnumSet.of(ChartTaskStatus.SUCCEEDED, ChartTaskStatus.WAITING, ChartTaskStatus.FAILED, ChartTaskStatus.CANCELED),
            ChartTaskStatus.FAILED, EnumSet.of(ChartTaskStatus.WAITING),
            ChartTaskStatus.SUCCEEDED, Set.of(), ChartTaskStatus.CANCELED, Set.of());

    private ChartTaskStateMachine() { }

    public static boolean canTransition(ChartTaskStatus from, ChartTaskStatus to) {
        return from != null && to != null && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
