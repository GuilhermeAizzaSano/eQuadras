package com.agendamentos.equadras.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class HttpRequestUtil {

    private HttpRequestUtil() {}

    public static HttpServletRequest obterRequestAtual() {
        RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    public static String extrairClientIp(HttpServletRequest request) {
        if (request == null) {
            HttpServletRequest current = obterRequestAtual();
            if (current != null) {
                return extrairClientIp(current);
            }
            return "127.0.0.1";
        }
        // O Tomcat (server.forward-headers-strategy=native + remoteip.internal-proxies) já resolve o IP real
        // a partir do X-Forwarded-For apenas quando a requisição vem do proxy confiável; ler o cabeçalho
        // diretamente permitiria que o cliente forjasse o próprio IP.
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "desconhecido";
    }

    public static String extrairUserAgent(HttpServletRequest request) {
        if (request == null) {
            HttpServletRequest current = obterRequestAtual();
            if (current != null) {
                return extrairUserAgent(current);
            }
            return "desconhecido";
        }
        String ua = request.getHeader("User-Agent");
        return (ua != null && !ua.isBlank()) ? ua : "desconhecido";
    }
}
