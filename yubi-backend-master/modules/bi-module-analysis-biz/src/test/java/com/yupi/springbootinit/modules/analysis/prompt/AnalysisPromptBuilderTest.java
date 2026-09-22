package com.yupi.springbootinit.modules.analysis.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 分析 Prompt 构造测试，不调用真实模型。 */
class AnalysisPromptBuilderTest {
    /** 测试用户目标、图表类型和 CSV 数据均进入用户消息。 */
    @Test
    void shouldBuildUserMessageWithBusinessInputs() {
        AnalysisPromptBuilder builder = new AnalysisPromptBuilder();
        String message = builder.buildUserMessage("分析地点分布", "柱状图", "地点,数量\n北京,10");
        assertTrue(message.contains("分析地点分布"));
        assertTrue(message.contains("柱状图"));
        assertTrue(message.contains("北京,10"));
    }
}
