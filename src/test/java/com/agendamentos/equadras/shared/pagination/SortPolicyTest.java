package com.agendamentos.equadras.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class SortPolicyTest {

  private final SortPolicy policy = SortPolicy.of(
      Set.of("dataHora", "id"), Sort.by(Sort.Direction.DESC, "dataHora"), "id");

  @Test
  void aplicaOrdenacaoPadraoComDesempateQuandoNaoHaSort() {
    Pageable result = policy.apply(PageRequest.of(2, 5));

    assertThat(result.getPageNumber()).isEqualTo(2);
    assertThat(result.getPageSize()).isEqualTo(5);
    assertThat(result.getSort()).isEqualTo(Sort.by(Sort.Order.desc("dataHora"), Sort.Order.desc("id")));
  }

  @Test
  void preservaSortPermitidoEAdicionaDesempateNaMesmaDirecao() {
    Pageable result = policy.apply(PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "dataHora")));

    assertThat(result.getSort()).isEqualTo(Sort.by(Sort.Order.asc("dataHora"), Sort.Order.asc("id")));
  }

  @Test
  void naoDuplicaDesempateQuandoJaInformado() {
    Pageable result = policy.apply(PageRequest.of(0, 5, Sort.by(Sort.Order.desc("dataHora"), Sort.Order.asc("id"))));

    assertThat(result.getSort()).isEqualTo(Sort.by(Sort.Order.desc("dataHora"), Sort.Order.asc("id")));
  }

  @Test
  void rejeitaPropriedadeForaDaWhitelist() {
    assertThatThrownBy(() -> policy.apply(PageRequest.of(0, 5, Sort.by("usuario.senha"))))
        .isInstanceOf(InvalidSortException.class);
  }
}
