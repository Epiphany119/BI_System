package com.yupi.springbootinit.airuntime;

import com.yupi.springbootinit.MainApplication;
import com.yupi.springbootinit.airuntime.api.AiChatClient;
import com.yupi.springbootinit.airuntime.api.AiChatRequest;
import com.yupi.springbootinit.airuntime.api.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 使用完整 Spring 配置验证智谱调用。
 * 默认跳过，只有明确传入 -DrunAiIntegration=true 才执行，避免普通测试误调用真实 API。
 */
@SpringBootTest(classes = MainApplication.class)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runAiIntegration", matches = "true")
class ZhipuAiChatIntegrationTest {

    @Autowired
    private AiChatClient aiChatClient;

    /** 验证 application-local.yml 中的 bi.ai.zhipu.api-key 能被 Spring 绑定并完成真实调用。 */
    @Test
    void shouldCallZhipuUsingApplicationLocalConfiguration() {
        AiChatRequest request = new AiChatRequest();
        request.setSystemPrompt("你是一个简洁的助手。");
        request.setUserMessage("请只回复：智谱集成测试成功");
        request.setTraceId("bi-web-ai-integration-test");

        AiChatResponse response = aiChatClient.chat(request);

        assertNotNull(response);
        assertFalse(response.getContent().isBlank());
        System.out.println("provider = " + response.getProvider());
        System.out.println("model = " + response.getModel());
        System.out.println("requestId = " + response.getRequestId());
        System.out.println("finishReason = " + response.getFinishReason());
        System.out.println("totalTokens = " + response.getTotalTokens());
        System.out.println("content = " + response.getContent());
    }
}
