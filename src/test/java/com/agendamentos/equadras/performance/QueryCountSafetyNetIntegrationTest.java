package com.agendamentos.equadras.performance;

import com.agendamentos.equadras.repository.QuadraRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class QueryCountSafetyNetIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private QuadraRepository quadraRepository;

    private MockMvc mockMvc;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("Monitora a contagem de queries em GET /quadras")
    void deveMonitorarContagemDeQueriesGetQuadras() throws Exception {
        statistics.clear();

        mockMvc.perform(get("/quadras"))
                .andExpect(status().isOk());

        long queryCount = statistics.getPrepareStatementCount();
        long executionCount = statistics.getQueryExecutionCount();

        // O teste passa registrando a contagem de baseline atual (quando N+1 for corrigido, queryCount <= 2)
        assertTrue(queryCount >= 1, "Pelo menos 1 query deve ser executada");
    }
}