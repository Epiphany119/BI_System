package com.yupi.springbootinit.modules.analysis.domain.repository;

import com.yupi.springbootinit.modules.analysis.domain.enums.ChartTaskStatus;
import com.yupi.springbootinit.modules.analysis.domain.model.ChartTask;

import java.util.Optional;

public interface ChartTaskRepository {
    Optional<ChartTask> findByTaskNo(String taskNo);
    ChartTask save(ChartTask task);
    boolean updateIfStatusMatches(ChartTask task, ChartTaskStatus expectedStatus);
}
