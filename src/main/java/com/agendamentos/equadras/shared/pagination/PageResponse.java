package com.agendamentos.equadras.shared.pagination;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Contrato estável de página. Não serializar Page/PageImpl diretamente. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  public static <S, T> PageResponse<T> of(Page<S> page, Function<? super S, ? extends T> mapper) {
    return of(page.map(mapper));
  }
}
