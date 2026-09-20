package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiResponseParserTest {

    @Test
    void supportsOriginalSeparatorFormat() {
        AiResponseParser.ParsedResult result = AiResponseParser.parse(
                "【【【【【\n{\"xAxis\":{}}\n【【【【【\n数据呈上升趋势");

        assertEquals("{\"xAxis\":{}}", result.getGenChart());
        assertEquals("数据呈上升趋势", result.getGenResult());
    }

    @Test
    void supportsJsonEnvelope() {
        AiResponseParser.ParsedResult result = AiResponseParser.parse(
                "{\"genChart\":{\"xAxis\":{}},\"genResult\":\"数据呈上升趋势\"}");

        assertEquals("{\"xAxis\":{}}", result.getGenChart());
        assertEquals("数据呈上升趋势", result.getGenResult());
    }

    @Test
    void supportsMarkdownChartAndConclusion() {
        AiResponseParser.ParsedResult result = AiResponseParser.parse(
                "```json\n{\"series\":[]}\n```\n分析结论：数据量较少");

        assertEquals("{\"series\":[]}", result.getGenChart());
        assertEquals("数据量较少", result.getGenResult());
    }

    @Test
    void rejectsUnstructuredAnswer() {
        assertThrows(IllegalArgumentException.class,
                () -> AiResponseParser.parse("这是一段没有图表配置的普通回答"));
    }
}
