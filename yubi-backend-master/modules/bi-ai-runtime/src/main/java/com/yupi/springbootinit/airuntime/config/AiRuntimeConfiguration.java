package com.yupi.springbootinit.airuntime.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiRuntimeProperties.class)
public class AiRuntimeConfiguration {
}
