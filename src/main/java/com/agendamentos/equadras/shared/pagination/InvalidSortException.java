package com.agendamentos.equadras.shared.pagination;

public class InvalidSortException extends RuntimeException {
  public InvalidSortException(String property) {
    super("Ordenação não permitida: " + property);
  }
}
