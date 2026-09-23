package com.agendamentos.equadras.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

  @Test
  void convertePageMantendoMetadados() {
    Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

    PageResponse<String> response = PageResponse.of(page);

    assertThat(response.content()).containsExactly("a", "b");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(5);
    assertThat(response.totalPages()).isEqualTo(3);
  }

  @Test
  void mapeiaConteudoSemPerderMetadados() {
    Page<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 2);

    PageResponse<String> response = PageResponse.of(page, String::valueOf);

    assertThat(response.content()).containsExactly("1", "2");
    assertThat(response.totalElements()).isEqualTo(2);
  }
}
