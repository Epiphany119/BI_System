package com.yupi.springbootinit.modules.analysis.prompt;

import org.springframework.stereotype.Component;

/** 只负责构造分析业务 Prompt，不包含供应商 HTTP 或模型协议细节。 */
@Component
public class AnalysisPromptBuilder {
    public String buildSystemPrompt() {
        return "你是数据分析师和前端 ECharts 专家。"
                + "请根据用户目标和原始表格数据生成分析结果。"
                + "只能输出一个合法 JSON 对象，不要输出 Markdown、标题或额外解释。"
                + "JSON 必须包含 genChart 和 genResult 两个字段；genChart 必须是 ECharts option 对象，genResult 必须是分析结论字符串。"
                + "所有 JSON 键名和字符串使用双引号，不能包含函数、undefined、NaN 或尾逗号。";
    }

    /** 将用户目标和已经受限的数据文本拼接为模型用户消息。 */
    public String buildUserMessage(String goal, String chartType, String csvData) {
        if (goal == null || goal.isBlank()) throw new IllegalArgumentException("分析目标不能为空");
        String actualGoal = chartType == null || chartType.isBlank() ? goal : goal + "，请使用" + chartType;
        return "分析需求：\n" + actualGoal + "\n原始数据：\n" + csvData;
    }
}
