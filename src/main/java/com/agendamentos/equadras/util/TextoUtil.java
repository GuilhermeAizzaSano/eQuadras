package com.agendamentos.equadras.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextoUtil {

    private TextoUtil() {}

    /** Remove acentos, converte para minúsculas e apara espaços. Retorna "" para null. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /** Escapa curingas de LIKE ('\', '%', '_') usando '\' como caractere de escape. */
    public static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
