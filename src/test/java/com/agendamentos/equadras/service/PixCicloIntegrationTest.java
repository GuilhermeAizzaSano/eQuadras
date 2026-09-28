package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.LogAuditoriaRepository;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNull;

/** M3: ao confirmar ou cancelar, as colunas de Pix são zeradas no banco. */
@SpringBootTest
class PixCicloIntegrationTest {

    @Autowired private AgendamentoService agendamentoService;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private NotificacaoRepository notificacaoRepository;
    @Autowired private LogAuditoriaRepository logAuditoriaRepository;

    @AfterEach
    void limpar() {
        notificacaoRepository.deleteAll();
        logAuditoriaRepository.deleteAll();
        agendamentoRepository.deleteAll();
        quadraRepository.deleteAll();
        usuarioRepository.findByEmail_usuario("cliente.pix@teste.com").ifPresent(usuarioRepository::delete);
        usuarioRepository.findByEmail_usuario("admin.pix@teste.com").ifPresent(usuarioRepository::delete);
    }

    @Test
    @DisplayName("M3: cancelar e confirmar por webhook limpam pix_copiaecola e qr_code_base64")
    void confirmarECancelarLimpamPix() {
        Usuario cliente = usuarioRepository.save(Usuario.builder().nome_usuario("Cliente").email_usuario("cliente.pix@teste.com")
                .senha_usuario("x").phone_usuario("11999990001").role(Role.CLIENT).build());
        Usuario admin = usuarioRepository.save(Usuario.builder().nome_usuario("Admin").email_usuario("admin.pix@teste.com")
                .senha_usuario("x").phone_usuario("11999990002").role(Role.ADMIN).build());
        Quadra quadra = quadraRepository.save(Quadra.builder().nome("Quadra Pix").tipoEsporte(TipoEsporte.FUTSAL)
                .valorHora(BigDecimal.TEN).ativa(true).admin(admin).build());

        LocalDateTime inicio = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Agendamento aCancelar = agendamentoRepository.save(pendenteComPix(cliente, quadra, inicio));
        Agendamento aConfirmar = agendamentoRepository.save(pendenteComPix(cliente, quadra, inicio.plusHours(2)));

        agendamentoService.cancelar(aCancelar.getId_agendamento(), cliente.getId_usuario());
        agendamentoService.confirmarPagamentoPorWebhook(aConfirmar.getId_agendamento(), "tx-m3");

        Agendamento cancelado = agendamentoRepository.findById(aCancelar.getId_agendamento()).orElseThrow();
        Agendamento confirmado = agendamentoRepository.findById(aConfirmar.getId_agendamento()).orElseThrow();
        assertNull(cancelado.getPixCopiaECola());
        assertNull(cancelado.getQrCodeBase64());
        assertNull(confirmado.getPixCopiaECola());
        assertNull(confirmado.getQrCodeBase64());
    }

    private Agendamento pendenteComPix(Usuario cliente, Quadra quadra, LocalDateTime inicio) {
        return Agendamento.builder().usuario(cliente).quadra(quadra)
                .dataHoraInicio(inicio).dataHoraFim(inicio.plusHours(1))
                .valorTotal(BigDecimal.TEN).status(StatusAgendamento.PENDENTE)
                .pixCopiaECola("pix").qrCodeBase64("qr").build();
    }
}
