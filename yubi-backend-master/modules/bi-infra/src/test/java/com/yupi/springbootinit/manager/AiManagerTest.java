package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiManagerTest {

    @Test
    void shouldExtractContentFromZhipuResponse() {
        String response = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"分析结论\"}}]}";

        assertEquals("分析结论", AiManager.extractContent(response));
    }

    @Test
    void shouldReportZhipuErrorResponse() {
        String response = "{\"error\":{\"message\":\"invalid api key\"}}";

        BusinessException exception = assertThrows(BusinessException.class,
                () -> AiManager.extractContent(response));

        assertEquals("智谱 AI 返回错误：invalid api key", exception.getMessage());
    }

    @Test
    void shouldReportNonJsonResponse() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> AiManager.extractContent("Bad Gateway"));

        assertEquals(true, exception.getMessage().startsWith("智谱 AI 返回格式异常："));
    }
}
