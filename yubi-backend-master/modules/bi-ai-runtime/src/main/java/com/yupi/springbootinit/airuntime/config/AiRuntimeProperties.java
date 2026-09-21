package com.yupi.springbootinit.airuntime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "bi.ai")
public class AiRuntimeProperties {
    private String provider = "zhipu";
    private String defaultModel = "glm-4-flash";
    private List<String> allowedModels = new ArrayList<>(List.of("glm-4-flash"));
    private BigDecimal temperature = new BigDecimal("0.2");
    private Integer connectTimeoutMs = 10000;
    private Integer readTimeoutMs = 60000;
    private boolean startupValidation = false;
    private Zhipu zhipu = new Zhipu();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public List<String> getAllowedModels() { return allowedModels; }
    public void setAllowedModels(List<String> allowedModels) { this.allowedModels = allowedModels; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public Integer getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public boolean isStartupValidation() { return startupValidation; }
    public void setStartupValidation(boolean startupValidation) { this.startupValidation = startupValidation; }
    public Zhipu getZhipu() { return zhipu; }
    public void setZhipu(Zhipu zhipu) { this.zhipu = zhipu; }

    public static class Zhipu {
        private String apiKey;
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
