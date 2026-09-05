package com.agendamentos.equadras.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OpenApiExportTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void exportOpenApiJson() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        Path path = Paths.get("openapi.json");
        Files.writeString(path, json, java.nio.charset.StandardCharsets.UTF_8);

        try {
            String yaml = mockMvc.perform(get("/v3/api-docs.yaml"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            Path yamlPath = Paths.get("docs", "api", "openapi.yaml");
            if (Files.exists(yamlPath.getParent())) {
                Files.writeString(yamlPath, yaml, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }
}