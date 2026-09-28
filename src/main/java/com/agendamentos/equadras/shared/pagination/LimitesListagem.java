package com.agendamentos.equadras.shared.pagination;

/** Teto aplicado no SQL às listagens legadas sem paginação (spec onda 2, M11). */
public final class LimitesListagem {
    public static final int MAXIMO_SEM_PAGINACAO = 200;

    private LimitesListagem() {}
}
