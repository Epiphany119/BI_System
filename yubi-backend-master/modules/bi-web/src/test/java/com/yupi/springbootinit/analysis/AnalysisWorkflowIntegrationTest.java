package com.yupi.springbootinit.analysis;

import com.yupi.springbootinit.MainApplication;
import com.yupi.springbootinit.modules.analysis.application.AnalysisTaskApplicationService;
import com.yupi.springbootinit.modules.analysis.application.AnalysisTaskCommand;
import com.yupi.springbootinit.modules.analysis.application.AnalysisTaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 分析模块 Spring 集成测试：真实读取 local 配置中的智谱密钥，验证“文件转换→模型调用→结果落库”闭环。
 * 默认关闭，手动执行时增加 -DrunAiIntegration=true，避免普通测试消耗真实模型额度。
 */
@SpringBootTest(classes = MainApplication.class)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runAiIntegration", matches = "true")
class AnalysisWorkflowIntegrationTest {

    @Autowired
    private AnalysisTaskApplicationService analysisTaskApplicationService;

    /** 使用最小 CSV 数据验证分析任务能完成并返回图表配置和业务结论。 */
    @Test
    void shouldCompleteAnalysisWorkflowWithConfiguredZhipuClient() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "integration.csv", "text/csv",
                "地点,店铺数量\n北京,10\n上海,8\n广州,6\n".getBytes());
        AnalysisTaskResult result = analysisTaskApplicationService.executeSynchronously(
                new AnalysisTaskCommand(file, "集成测试图表", "分析不同地点店铺数量分布", "柱状图", 1L));

        assertNotNull(result);
        assertNotNull(result.getChartId());
        assertFalse(result.getGenChart().isBlank());
        assertFalse(result.getGenResult().isBlank());
    }
}
