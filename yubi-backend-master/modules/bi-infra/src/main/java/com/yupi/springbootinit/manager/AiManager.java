package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于对接 AI 平台
 */
@Service
public class AiManager {

    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private static final String ANALYSIS_SYSTEM_PROMPT = "你是数据分析师和前端 ECharts 专家。"
            + "请根据用户提供的分析需求和原始数据生成结果。"
            + "只能输出一个合法 JSON 对象，不要输出 Markdown 代码块、标题、解释或其他额外文字。"
            + "JSON 必须包含 genChart 和 genResult 两个字段；genChart 的值是 ECharts 5 option 对象，genResult 的值是详细、具体的分析结论字符串。"
            + "格式示例：{\"genChart\":{\"title\":{\"text\":\"示例\"}},\"genResult\":\"示例结论\"}。"
            + "genChart 必须是对象而不是字符串；所有键名和字符串都使用双引号，不能包含注释、函数、NaN、undefined 或尾逗号。"
            + "genResult 必须是字符串。";

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${zhipu.base-url:" + DEFAULT_BASE_URL + "}")
    private String baseUrl;

    @Value("${zhipu.model:glm-4-flash}")
    private String model;

    @Value("${zhipu.timeout-ms:60000}")
    private int timeoutMs;

    /**
     * AI 对话
     *
     * @param modelId
     * @param message
     * @return
     */
    public String doChat(long modelId, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "未配置智谱 AI API Key，请在 application-local.yml 的 zhipu.api-key 中填写");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", ANALYSIS_SYSTEM_PROMPT),
                Map.of("role", "user", "content", message)));
        payload.put("stream", false);
        payload.put("temperature", 0.2);

        try (HttpResponse response = HttpRequest.post(baseUrl)
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .timeout(timeoutMs)
                .body(JSONUtil.toJsonStr(payload))
                .execute()) {
            String responseBody = response.body();
            if (!response.isOk()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "智谱 AI 调用失败（HTTP " + response.getStatus() + "）：" + summarize(responseBody));
            }
            return extractContent(responseBody);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "智谱 AI 调用异常：" + e.getMessage());
        }
    }

    /**
     * 从智谱 OpenAI 兼容接口响应中提取首个候选答案。
     * 保留为包可见，便于单元测试而不发起真实网络请求。
     */
    static String extractContent(String responseBody) {
        try {
            JSONObject response = JSONUtil.parseObj(responseBody);
            JSONObject error = response.getJSONObject("error");
            if (error != null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "智谱 AI 返回错误：" + error.getStr("message", "未知错误"));
            }
            JSONArray choices = response.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "智谱 AI 未返回有效结果");
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message == null ? null : message.getStr("content");
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "智谱 AI 返回内容为空");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "智谱 AI 返回格式异常：" + summarize(responseBody));
        }
    }

    private static String summarize(String body) {
        if (body == null || body.isBlank()) {
            return "空响应";
        }
        String normalized = body.replaceAll("\\s+", " ");
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300) + "...";
    }
}
