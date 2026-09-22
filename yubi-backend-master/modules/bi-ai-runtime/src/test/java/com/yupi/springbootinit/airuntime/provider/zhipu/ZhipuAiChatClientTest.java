package com.yupi.springbootinit.airuntime.provider.zhipu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.springbootinit.airuntime.api.AiChatRequest;
import com.yupi.springbootinit.airuntime.api.AiChatResponse;
import com.yupi.springbootinit.airuntime.config.AiRuntimeProperties;
import com.yupi.springbootinit.airuntime.exception.AiRuntimeErrorCode;
import com.yupi.springbootinit.airuntime.exception.AiRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智谱客户端测试集。
 *
 * 前三个测试不访问网络，用于验证 Runtime 的本地参数和配置校验；
 * 最后一个测试只有设置 ZHIPU_API_KEY 后才会执行，用于手动验证真实智谱接口。
 */
class ZhipuAiChatClientTest {

    /** 测试请求指定了不在白名单中的模型时，Runtime 是否在发起网络请求前拒绝。 */
    @Test
    void shouldRejectModelOutsideAllowListWithoutCallingNetwork() {
        AiRuntimeProperties properties = properties("test-key");
        properties.setAllowedModels(List.of("glm-4-flash"));
        ZhipuAiChatClient client = client(properties);

        AiChatRequest request = request("hello");
        request.setModel("not-allowed-model");

        AiRuntimeException exception = assertThrows(AiRuntimeException.class, () -> client.chat(request));
        assertEquals(AiRuntimeErrorCode.MODEL_NOT_ALLOWED, exception.getErrorCode());
        assertFalse(exception.isRetryable());
    }

    /** 测试用户消息为空时，Runtime 是否返回配置错误而不是调用上游。 */
    @Test
    void shouldRejectBlankUserMessageWithoutCallingNetwork() {
        ZhipuAiChatClient client = client(properties("test-key"));
        AiChatRequest request = request(" ");

        AiRuntimeException exception = assertThrows(AiRuntimeException.class, () -> client.chat(request));
        assertEquals(AiRuntimeErrorCode.AI_CONFIGURATION_ERROR, exception.getErrorCode());
        assertFalse(exception.isRetryable());
    }

    /** 测试 API Key 缺失时，调用阶段是否返回 AI_CONFIGURATION_ERROR。 */
    @Test
    void shouldRejectMissingApiKeyWhenCalling() {
        ZhipuAiChatClient client = client(properties(""));

        AiRuntimeException exception = assertThrows(AiRuntimeException.class,
                () -> client.chat(request("hello")));
        assertEquals(AiRuntimeErrorCode.AI_CONFIGURATION_ERROR, exception.getErrorCode());
        assertFalse(exception.isRetryable());
    }

    /**
     * 手动真实接口测试：验证 API Key、HTTP 请求、智谱响应解析和 Token 用量解析。
     * 未设置 ZHIPU_API_KEY 时自动跳过，不影响普通单元测试。
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ZHIPU_API_KEY", matches = ".+")
    void shouldCallRealZhipuApi() {
        AiRuntimeProperties properties = properties(System.getenv("ZHIPU_API_KEY"));
        ZhipuAiChatClient client = client(properties);

        AiChatRequest request = request("请只回复：智谱接口调用成功");
        request.setSystemPrompt("你是一个简洁的助手。");
        request.setTraceId("bi-ai-runtime-manual-test");

        AiChatResponse response = client.chat(request);

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().isBlank());
        assertEquals("zhipu", response.getProvider());

        System.out.println("provider = " + response.getProvider());
        System.out.println("model = " + response.getModel());
        System.out.println("requestId = " + response.getRequestId());
        System.out.println("finishReason = " + response.getFinishReason());
        System.out.println("inputTokens = " + response.getInputTokens());
        System.out.println("outputTokens = " + response.getOutputTokens());
        System.out.println("totalTokens = " + response.getTotalTokens());
        System.out.println("content = " + response.getContent());
    }

    /** 创建被测客户端，使用真实的 Java 对象依赖，不启动完整 Spring 容器。 */
    private static ZhipuAiChatClient client(AiRuntimeProperties properties) {
        return new ZhipuAiChatClient(properties, new ObjectMapper());
    }

    /** 构造测试配置；测试中使用的 test-key 不会被发送，因为相关用例会在本地校验阶段结束。 */
    private static AiRuntimeProperties properties(String apiKey) {
        AiRuntimeProperties properties = new AiRuntimeProperties();
        properties.setProvider("zhipu");
        properties.setDefaultModel("glm-4-flash");
        properties.setAllowedModels(List.of("glm-4-flash", "glm-4-plus"));
        properties.getZhipu().setApiKey(apiKey);
        return properties;
    }

    /** 构造最小合法请求，调用方可在具体测试中继续补充模型、Prompt 和 traceId。 */
    private static AiChatRequest request(String message) {
        AiChatRequest request = new AiChatRequest();
        request.setUserMessage(message);
        return request;
    }
}
