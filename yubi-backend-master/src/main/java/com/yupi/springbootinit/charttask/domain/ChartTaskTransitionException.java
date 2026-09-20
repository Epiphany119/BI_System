package com.yupi.springbootinit.charttask.domain;

public class ChartTaskTransitionException extends IllegalStateException {
    public ChartTaskTransitionException(ChartTaskStatus from, ChartTaskStatus to) {
        super("Illegal chart task transition: " + from + " -> " + to);
    }
}
