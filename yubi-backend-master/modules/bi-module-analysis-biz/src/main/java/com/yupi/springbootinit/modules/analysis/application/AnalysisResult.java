package com.yupi.springbootinit.modules.analysis.application;

/** 业务层解析后的统一分析结果，不暴露智谱供应商响应结构。 */
public class AnalysisResult {
    private final String genChart;
    private final String genResult;

    public AnalysisResult(String genChart, String genResult) {
        this.genChart = genChart;
        this.genResult = genResult;
    }

    public String getGenChart() { return genChart; }
    public String getGenResult() { return genResult; }
}
