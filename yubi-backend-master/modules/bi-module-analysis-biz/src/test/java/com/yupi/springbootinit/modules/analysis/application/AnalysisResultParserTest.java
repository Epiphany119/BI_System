package com.yupi.springbootinit.modules.analysis.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 分析业务结果解析测试，不调用真实 AI。 */
class AnalysisResultParserTest {
    private AnalysisResultParser parser;

    @BeforeEach
    void setUp() { parser = new AnalysisResultParser(new ObjectMapper()); }

    /** 测试标准 JSON 响应能够提取图表配置和分析结论。 */
    @Test
    void shouldParseStandardJson() {
        AnalysisResult result = parser.parse("{\"genChart\":{\"title\":{\"text\":\"测试\"}},\"genResult\":\"结论\"}");
        assertTrue(result.getGenChart().contains("测试"));
        assertEquals("结论", result.getGenResult());
    }

    /** 测试模型返回 Markdown JSON 代码块时能够兼容。 */
    @Test
    void shouldParseMarkdownJson() {
        AnalysisResult result = parser.parse("```json\n{\"genChart\":{},\"genResult\":\"结论\"}\n```");
        assertEquals("结论", result.getGenResult());
    }

    /** 测试历史五重分隔符格式能够兼容。 */
    @Test
    void shouldParseLegacyDelimiter() {
        AnalysisResult result = parser.parse("【【【【【\n{\"xAxis\":{}}\n【【【【【\n历史结论");
        assertEquals("历史结论", result.getGenResult());
    }

    /** 测试缺少业务字段时必须拒绝，避免保存半成品结果。 */
    @Test
    void shouldRejectMissingBusinessFields() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"genChart\":{},\"other\":\"x\"}"));
    }
}
