package com.agendamentos.equadras.shared.pagination;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Política de ordenação por endpoint: whitelist, ordenação padrão e desempate determinístico
 * (sem desempate, a paginação por offset pode duplicar ou pular registros).
 */
public record SortPolicy(Set<String> allowedProperties, Sort defaultSort, String tiebreaker) {

  public static SortPolicy of(Set<String> allowedProperties, Sort defaultSort, String tiebreaker) {
    return new SortPolicy(Set.copyOf(allowedProperties), defaultSort, tiebreaker);
  }

  public Pageable apply(Pageable requested) {
    Sort sort = requested.getSort().isSorted() ? requested.getSort() : defaultSort;

    for (Sort.Order order : sort) {
      if (!allowedProperties.contains(order.getProperty())) {
        throw new InvalidSortException(order.getProperty());
      }
    }

    if (sort.getOrderFor(tiebreaker) == null) {
      Sort.Direction direction = sort.stream()
          .reduce((first, second) -> second)
          .map(Sort.Order::getDirection)
          .orElse(Sort.Direction.DESC);
      sort = sort.and(Sort.by(direction, tiebreaker));
    }

    return PageRequest.of(requested.getPageNumber(), requested.getPageSize(), sort);
  }
}
