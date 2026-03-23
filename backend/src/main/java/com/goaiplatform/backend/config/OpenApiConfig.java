package com.goaiplatform.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI().info(new Info()
                .title("GO-AI API")
                .version("1.0.0")
                .description("多厂商 AI 对话平台 API，覆盖认证、实时SSE对话、OpenAI密钥安全管理、会话导出与使用统计能力"));
    }
}
