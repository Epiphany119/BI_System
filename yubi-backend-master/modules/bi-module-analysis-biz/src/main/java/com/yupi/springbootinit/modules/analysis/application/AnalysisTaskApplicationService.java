package com.yupi.springbootinit.modules.analysis.application;

import com.yupi.springbootinit.airuntime.api.AiChatClient;
import com.yupi.springbootinit.airuntime.api.AiChatRequest;
import com.yupi.springbootinit.airuntime.api.AiChatResponse;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.modules.analysis.converter.SpreadsheetDataConverter;
import com.yupi.springbootinit.modules.analysis.prompt.AnalysisPromptBuilder;
import com.yupi.springbootinit.service.ChartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 智能分析唯一业务编排入口，Controller 和 MQ Consumer 都应复用它。 */
@Service
public class AnalysisTaskApplicationService {
    public static final String WAITING = "WAITING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";

    private final ChartService chartService;
    private final AiChatClient aiChatClient;
    private final SpreadsheetDataConverter dataConverter;
    private final AnalysisPromptBuilder promptBuilder;
    private final AnalysisResultParser resultParser;

    public AnalysisTaskApplicationService(ChartService chartService, AiChatClient aiChatClient,
                                          SpreadsheetDataConverter dataConverter,
                                          AnalysisPromptBuilder promptBuilder,
                                          AnalysisResultParser resultParser) {
        this.chartService = chartService; this.aiChatClient = aiChatClient;
        this.dataConverter = dataConverter; this.promptBuilder = promptBuilder; this.resultParser = resultParser;
    }

    /** 执行同步分析：创建任务、调用模型、解析并保存成功结果。 */
    @Transactional
    public AnalysisTaskResult executeSynchronously(AnalysisTaskCommand command) {
        validate(command);
        String csvData = dataConverter.convert(command.getFile());
        Chart chart = new Chart();
        chart.setName(command.getName()); chart.setGoal(command.getGoal()); chart.setChartType(command.getChartType());
        chart.setChartData(csvData); chart.setUserId(command.getUserId()); chart.setStatus(WAITING);
        if (!chartService.save(chart)) throw new IllegalStateException("分析任务创建失败");
        chart.setStatus(RUNNING); chartService.updateById(chart);
        try {
            AiChatRequest request = new AiChatRequest();
            request.setSystemPrompt(promptBuilder.buildSystemPrompt());
            request.setUserMessage(promptBuilder.buildUserMessage(command.getGoal(), command.getChartType(), csvData));
            request.setTraceId(String.valueOf(chart.getId()));
            AiChatResponse response = aiChatClient.chat(request);
            AnalysisResult result = resultParser.parse(response.getContent());
            chart.setGenChart(result.getGenChart()); chart.setGenResult(result.getGenResult()); chart.setStatus(SUCCEEDED); chart.setExecMessage(null);
            if (!chartService.updateById(chart)) throw new IllegalStateException("分析结果保存失败");
            return new AnalysisTaskResult(chart.getId(), result.getGenChart(), result.getGenResult());
        } catch (RuntimeException e) {
            chart.setStatus(FAILED); chart.setExecMessage(sanitize(e.getMessage())); chartService.updateById(chart);
            throw e;
        }
    }

    private void validate(AnalysisTaskCommand command) {
        if (command == null || command.getUserId() == null) throw new IllegalArgumentException("用户不能为空");
        if (command.getFile() == null || command.getFile().isEmpty()) throw new IllegalArgumentException("文件不能为空");
        if (command.getGoal() == null || command.getGoal().isBlank()) throw new IllegalArgumentException("分析目标不能为空");
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) return "分析执行失败";
        String value = message.replaceAll("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]\\s*\\S+", "$1=[REDACTED]");
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
