package com.agendamentos.equadras.security;

import java.util.Set;

/**
 * Rotas isentas das validações estritas de X-Client e de esquema de Authorization
 * (webhooks e bots externos que não conseguem enviar esses cabeçalhos).
 * Comparação deve ser sempre exata (equals), nunca contains/startsWith, para evitar bypass.
 */
public final class SecurityRoutes {

    public static final Set<String> ROTAS_ISENTAS = Set.of(
            "/pagamentos/webhook",
            "/api/pagamentos/webhook",
            "/agendamentos/bot",
            "/api/agendamentos/bot"
    );

    private SecurityRoutes() {
    }

    public static boolean isRotaIsenta(String uri) {
        return uri != null && ROTAS_ISENTAS.contains(uri);
    }
}
