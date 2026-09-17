package com.agendamentos.equadras.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json;charset=UTF-8");
        String mensagem = authException != null && authException.getMessage() != null
                ? authException.getMessage()
                : "Acesso não autorizado: credencial ausente, expirada ou inválida.";

        response.getWriter().write(String.format("""
            {
                "status": 401,
                "title": "Não Autorizado",
                "detail": "%s"
            }
            """, mensagem.replace("\"", "\\\"")));
    }
}
