package com.yupi.springbootinit.modules.analysis.domain.service;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskStatus;

public class ChartTaskTransitionException extends IllegalStateException {
    public ChartTaskTransitionException(ChartTaskStatus from, ChartTaskStatus to) {
        super("Illegal chart task transition: " + from + " -> " + to);
    }
}
