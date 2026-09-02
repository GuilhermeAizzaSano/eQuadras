package com.agendamentos.equadras.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @org.springframework.beans.factory.annotation.Value("${equadras.cors.origens-permitidas:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000}")
    private String[] origensPermitidas;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }

    @Override
    public void addArgumentResolvers(java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new com.agendamentos.equadras.security.UsuarioLogadoArgumentResolver());
    }
}