package com.agendamentos.equadras.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json;charset=UTF-8");
        String mensagem = accessDeniedException != null && accessDeniedException.getMessage() != null
                ? accessDeniedException.getMessage()
                : "Acesso proibido: sua credencial não possui permissão para executar esta operação.";

        response.getWriter().write(String.format("""
            {
                "status": 403,
                "title": "Acesso Proibido",
                "detail": "%s"
            }
            """, mensagem.replace("\"", "\\\"")));
    }
}
