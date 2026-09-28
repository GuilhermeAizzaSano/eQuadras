package com.agendamentos.equadras.util;

/** Telefone brasileiro guardado só com dígitos: DDD + número (10 ou 11 dígitos). */
public final class TelefoneUtil {

    private TelefoneUtil() {}

    public static String normalizar(String telefone) {
        if (telefone == null) return null;
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.isEmpty()) return null;
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            digitos = digitos.substring(2);
        }
        return digitos;
    }
}
