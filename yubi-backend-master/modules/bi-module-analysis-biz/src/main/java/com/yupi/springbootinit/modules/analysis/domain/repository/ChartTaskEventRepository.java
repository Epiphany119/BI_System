package com.yupi.springbootinit.modules.analysis.domain.repository;

import com.yupi.springbootinit.modules.analysis.domain.model.ChartTaskEvent;

public interface ChartTaskEventRepository {
    void save(ChartTaskEvent event);
}
