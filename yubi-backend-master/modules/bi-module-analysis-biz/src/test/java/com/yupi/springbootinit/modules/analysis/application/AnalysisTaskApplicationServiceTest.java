package com.yupi.springbootinit.modules.analysis.application;

import com.yupi.springbootinit.airuntime.api.AiChatClient;
import com.yupi.springbootinit.airuntime.api.AiChatResponse;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.modules.analysis.converter.SpreadsheetDataConverter;
import com.yupi.springbootinit.modules.analysis.prompt.AnalysisPromptBuilder;
import com.yupi.springbootinit.service.ChartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 应用服务测试：使用 Mock AI，验证成功和失败状态落库。 */
class AnalysisTaskApplicationServiceTest {
    /** 测试完整同步成功链路会保存图表结果并返回业务结果。 */
    @Test
    void shouldSaveSucceededChartAfterAiResponse() {
        ChartService chartService = mock(ChartService.class);
        when(chartService.save(any(Chart.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Chart.class).setId(1L); return true;
        });
        when(chartService.updateById(any(Chart.class))).thenReturn(true);
        AiChatClient aiClient = mock(AiChatClient.class);
        AiChatResponse response = new AiChatResponse();
        response.setContent("{\"genChart\":{},\"genResult\":\"分析完成\"}");
        when(aiClient.chat(any())).thenReturn(response);

        AnalysisTaskApplicationService service = new AnalysisTaskApplicationService(
                chartService, aiClient, new SpreadsheetDataConverter(), new AnalysisPromptBuilder(),
                new AnalysisResultParser(new ObjectMapper()));
        AnalysisTaskResult result = service.executeSynchronously(command());

        assertEquals(1L, result.getChartId());
        assertEquals("分析完成", result.getGenResult());
        verify(chartService, atLeastOnce()).updateById(argThat(chart -> AnalysisTaskApplicationService.SUCCEEDED.equals(chart.getStatus())));
    }

    /** 测试 AI 调用失败会把已创建任务更新为 FAILED，并向上层继续抛出异常。 */
    @Test
    void shouldMarkChartFailedWhenAiCallFails() {
        ChartService chartService = mock(ChartService.class);
        when(chartService.save(any(Chart.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Chart.class).setId(2L); return true;
        });
        when(chartService.updateById(any(Chart.class))).thenReturn(true);
        AiChatClient aiClient = mock(AiChatClient.class);
        when(aiClient.chat(any())).thenThrow(new IllegalStateException("mock ai failure"));
        AnalysisTaskApplicationService service = new AnalysisTaskApplicationService(
                chartService, aiClient, new SpreadsheetDataConverter(), new AnalysisPromptBuilder(),
                new AnalysisResultParser(new ObjectMapper()));

        assertThrows(IllegalStateException.class, () -> service.executeSynchronously(command()));
        verify(chartService, atLeastOnce()).updateById(argThat(chart -> AnalysisTaskApplicationService.FAILED.equals(chart.getStatus())));
    }

    private AnalysisTaskCommand command() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "地点,数量\n北京,10\n".getBytes());
        return new AnalysisTaskCommand(file, "测试图表", "分析地点分布", "柱状图", 100L);
    }
}
