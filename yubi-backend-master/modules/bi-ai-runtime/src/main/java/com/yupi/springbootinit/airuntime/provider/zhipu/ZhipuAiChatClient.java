package com.yupi.springbootinit.airuntime.provider.zhipu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.springbootinit.airuntime.api.AiChatClient;
import com.yupi.springbootinit.airuntime.api.AiChatRequest;
import com.yupi.springbootinit.airuntime.api.AiChatResponse;
import com.yupi.springbootinit.airuntime.config.AiRuntimeProperties;
import com.yupi.springbootinit.airuntime.exception.AiRuntimeErrorCode;
import com.yupi.springbootinit.airuntime.exception.AiRuntimeException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ZhipuAiChatClient implements AiChatClient {
    private final AiRuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ZhipuAiChatClient(AiRuntimeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        if (properties.isStartupValidation()) {
            validateConfiguration(null);
        }
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (request == null || request.getUserMessage() == null || request.getUserMessage().isBlank()) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_CONFIGURATION_ERROR, false, "userMessage 不能为空");
        }
        validateConfiguration(request.getTraceId());
        String model = resolveModel(request);
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("stream", false);
        payload.put("temperature", request.getTemperature() == null ? properties.getTemperature() : request.getTemperature());
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.getUserMessage()));
        payload.put("messages", messages);
        if (request.getMaxTokens() != null) payload.put("max_tokens", request.getMaxTokens());

        long started = System.nanoTime();
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(properties.getZhipu().getBaseUrl()))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Authorization", "Bearer " + properties.getZhipu().getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String requestId = response.headers().firstValue("X-Request-ID").orElse(null);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw mapHttpError(response.statusCode(), response.body(), requestId, request.getTraceId());
            }
            return parseResponse(response.body(), requestId, request.getTraceId());
        } catch (AiRuntimeException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_TIMEOUT, true, null, null, "智谱请求超时", request.getTraceId(), e);
        } catch (java.io.IOException e) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_NETWORK_ERROR, true, null, null, "智谱网络请求失败", request.getTraceId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_NETWORK_ERROR, true, null, null, "智谱请求被中断", request.getTraceId(), e);
        } catch (Exception e) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_UNKNOWN_ERROR, false, null, null, "智谱调用失败", request.getTraceId(), e);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            // Keep the first version dependency-free: callers can add metrics around this client.
            if (durationMs < 0) throw new IllegalStateException("invalid duration");
        }
    }

    private String resolveModel(AiChatRequest request) {
        String model = request.getModel() == null || request.getModel().isBlank() ? properties.getDefaultModel() : request.getModel();
        if (properties.getAllowedModels() == null || !properties.getAllowedModels().contains(model)) {
            throw new AiRuntimeException(AiRuntimeErrorCode.MODEL_NOT_ALLOWED, false, "模型不在允许列表中");
        }
        return model;
    }

    private void validateConfiguration(String traceId) {
        if (!"zhipu".equalsIgnoreCase(properties.getProvider())) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_CONFIGURATION_ERROR, false, "不支持的 AI provider");
        }
        if (properties.getZhipu() == null || properties.getZhipu().getApiKey() == null || properties.getZhipu().getApiKey().isBlank()) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_CONFIGURATION_ERROR, false, "未配置智谱 AI API Key");
        }
        if (properties.getZhipu().getBaseUrl() == null || properties.getZhipu().getBaseUrl().isBlank()) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_CONFIGURATION_ERROR, false, "未配置智谱 AI Base URL");
        }
    }

    private AiRuntimeException mapHttpError(int status, String body, String requestId, String traceId) {
        AiRuntimeErrorCode code = status == 401 || status == 403 ? AiRuntimeErrorCode.AI_AUTHENTICATION_ERROR
                : status == 429 ? AiRuntimeErrorCode.AI_RATE_LIMITED
                : status >= 500 ? AiRuntimeErrorCode.AI_UPSTREAM_UNAVAILABLE
                : AiRuntimeErrorCode.AI_UNKNOWN_ERROR;
        boolean retryable = code == AiRuntimeErrorCode.AI_RATE_LIMITED || code == AiRuntimeErrorCode.AI_UPSTREAM_UNAVAILABLE;
        return new AiRuntimeException(code, retryable, status, requestId, summarize(body), traceId, null);
    }

    private AiChatResponse parseResponse(String body, String requestId, String traceId) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) throw invalid("智谱未返回有效 choices", traceId);
            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText(null);
            if (content == null || content.isBlank()) throw invalid("智谱返回内容为空", traceId);
            AiChatResponse result = new AiChatResponse();
            result.setContent(content);
            result.setProvider("zhipu");
            result.setModel(root.path("model").asText(properties.getDefaultModel()));
            result.setRequestId(requestId != null ? requestId : root.path("id").asText(null));
            result.setFinishReason(choices.get(0).path("finish_reason").asText(null));
            JsonNode usage = root.path("usage");
            result.setInputTokens(integerOrNull(usage, "prompt_tokens"));
            result.setOutputTokens(integerOrNull(usage, "completion_tokens"));
            result.setTotalTokens(integerOrNull(usage, "total_tokens"));
            return result;
        } catch (AiRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AiRuntimeException(AiRuntimeErrorCode.AI_RESPONSE_INVALID, false, null, requestId, "智谱响应格式异常", traceId, e);
        }
    }

    private AiRuntimeException invalid(String message, String traceId) {
        return new AiRuntimeException(AiRuntimeErrorCode.AI_RESPONSE_INVALID, false, null, null, message, traceId, null);
    }

    private Integer integerOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asInt() : null;
    }

    private String summarize(String body) {
        if (body == null || body.isBlank()) return "空响应";
        String normalized = body.replaceAll("\\s+", " ");
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }
}
