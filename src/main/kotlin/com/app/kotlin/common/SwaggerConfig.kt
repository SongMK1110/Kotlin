package com.app.kotlin.common

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(info = Info(title = "날씨 API 명세서", description = "날씨 API 명세서", version = "v1.0.0"))
@Configuration
class SwaggerConfig {
}