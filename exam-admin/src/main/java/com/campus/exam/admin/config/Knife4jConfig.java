package com.campus.exam.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校内在线考试平台 API")
                        .version("1.0.0")
                        .description("题库 / 智能组卷 / 考试 / 异步收卷 / 阅卷统计"))
                .components(new Components().addSecuritySchemes("Authorization",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));
    }
}
