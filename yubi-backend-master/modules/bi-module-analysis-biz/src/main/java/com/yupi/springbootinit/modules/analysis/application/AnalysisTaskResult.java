package com.yupi.springbootinit.modules.analysis.application;

/** 同步分析成功后的业务返回值。 */
public class AnalysisTaskResult {
    private final Long chartId;
    private final String genChart;
    private final String genResult;

    public AnalysisTaskResult(Long chartId, String genChart, String genResult) {
        this.chartId = chartId; this.genChart = genChart; this.genResult = genResult;
    }
    public Long getChartId() { return chartId; }
    public String getGenChart() { return genChart; }
    public String getGenResult() { return genResult; }
}
