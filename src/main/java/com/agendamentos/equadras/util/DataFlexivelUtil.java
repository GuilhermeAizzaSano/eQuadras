package com.agendamentos.equadras.util;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class DataFlexivelUtil {

    public static final ZoneId ZONE_BRASIL = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter[] FORMATADORES = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
    };

    /**
     * Resolve uma string de data flexível para LocalDate.
     * Suporta:
     * - null ou vazio (retorna null para busca automática do próximo dia livre)
     * - Termos relativos: 'hoje', 'amanha', 'depois de amanha'
     * - Dias da semana: 'segunda', 'terca', 'quarta', 'quinta', 'sexta', 'sabado', 'domingo'
     * - Formatos numéricos: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy, dd/MM
     */
    public static LocalDate resolverData(String textoData) {
        if (textoData == null || textoData.isBlank()) {
            return null;
        }

        LocalDate hoje = LocalDate.now(ZONE_BRASIL);
        String texto = textoData.trim();

        // 1. Tenta formatadores padrão com ano
        for (DateTimeFormatter fmt : FORMATADORES) {
            try {
                return LocalDate.parse(texto, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }

        // 2. Formato dd/MM ou dd-MM (sem ano)
        if ((texto.contains("/") || texto.contains("-")) && texto.matches("^\\d{1,2}[/\\-]\\d{1,2}$")) {
            try {
                String[] partes = texto.split("[/\\-]");
                int dia = Integer.parseInt(partes[0].trim());
                int mes = Integer.parseInt(partes[1].trim());
                LocalDate dataMontada = LocalDate.of(hoje.getYear(), mes, dia);
                if (dataMontada.isBefore(hoje)) {
                    dataMontada = dataMontada.plusYears(1);
                }
                return dataMontada;
            } catch (Exception ignored) {
            }
        }

        // 3. Normalização para termos e dias da semana
        String limpo = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("feira", "")
                .replace("-", "")
                .replace(" ", "")
                .trim();

        if (limpo.equals("hoje") || limpo.equals("today")) {
            return hoje;
        }
        if (limpo.equals("amanha") || limpo.equals("tomorrow")) {
            return hoje.plusDays(1);
        }
        if (limpo.equals("depoisdeamanha")) {
            return hoje.plusDays(2);
        }

        // 4. Dias da semana
        DayOfWeek diaSemana = extrairDiaSemana(limpo);
        if (diaSemana != null) {
            if (hoje.getDayOfWeek() == diaSemana) {
                LocalDateTime agora = LocalDateTime.now(ZONE_BRASIL);
                if (agora.getHour() < 18) {
                    return hoje;
                }
                return hoje.plusWeeks(1);
            }
            return hoje.with(TemporalAdjusters.next(diaSemana));
        }

        return hoje;
    }

    private static DayOfWeek extrairDiaSemana(String limpo) {
        if (limpo.startsWith("seg")) return DayOfWeek.MONDAY;
        if (limpo.startsWith("ter")) return DayOfWeek.TUESDAY;
        if (limpo.startsWith("qua")) return DayOfWeek.WEDNESDAY;
        if (limpo.startsWith("qui")) return DayOfWeek.THURSDAY;
        if (limpo.startsWith("sex")) return DayOfWeek.FRIDAY;
        if (limpo.startsWith("sab")) return DayOfWeek.SATURDAY;
        if (limpo.startsWith("dom")) return DayOfWeek.SUNDAY;
        return null;
    }
}
