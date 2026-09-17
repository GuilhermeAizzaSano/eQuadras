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
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
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
