package com.yupi.springbootinit.modules.analysis.application;

import org.springframework.web.multipart.MultipartFile;

/** 创建分析任务所需的业务输入。用户 ID 必须来自登录上下文。 */
public class AnalysisTaskCommand {
    private final MultipartFile file;
    private final String name;
    private final String goal;
    private final String chartType;
    private final Long userId;

    public AnalysisTaskCommand(MultipartFile file, String name, String goal, String chartType, Long userId) {
        this.file = file; this.name = name; this.goal = goal; this.chartType = chartType; this.userId = userId;
    }
    public MultipartFile getFile() { return file; }
    public String getName() { return name; }
    public String getGoal() { return goal; }
    public String getChartType() { return chartType; }
    public Long getUserId() { return userId; }
}
