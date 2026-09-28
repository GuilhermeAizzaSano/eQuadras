package com.agendamentos.equadras.dto.response;

import com.agendamentos.equadras.model.entity.Notificacao;
import com.agendamentos.equadras.model.entity.Usuario;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacaoResponseDTOTest {

    @Test
    void deveMapearCamposSemExporAdmin() {
        Usuario admin = Usuario.builder().nome_usuario("Admin").email_usuario("admin@teste.com").build();
        Notificacao notificacao = new Notificacao(admin, "Nova reserva");
        notificacao.setId(7L);
        notificacao.setLida(true);
        notificacao.setDataCriacao(LocalDateTime.of(2026, 9, 28, 10, 0));

        NotificacaoResponseDTO dto = NotificacaoResponseDTO.fromEntity(notificacao);
        String json = JsonMapper.builder().findAndAddModules().build().writeValueAsString(dto);

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.mensagem()).isEqualTo("Nova reserva");
        assertThat(dto.lida()).isTrue();
        assertThat(dto.excluida()).isFalse();
        assertThat(dto.dataCriacao()).isEqualTo(LocalDateTime.of(2026, 9, 28, 10, 0));
        assertThat(json).doesNotContain("admin").doesNotContain("email");
    }
}
