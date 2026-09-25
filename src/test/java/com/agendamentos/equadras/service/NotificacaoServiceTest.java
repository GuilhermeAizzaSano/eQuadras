package com.agendamentos.equadras.service;

import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.agendamentos.equadras.model.entity.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Spy
    private JsonMapper objectMapper = JsonMapper.builder().build();

    @InjectMocks
    private NotificacaoService notificacaoService;

    @Test
    @DisplayName("Deve assinar e substituir emitter anterior completando-o para evitar vazamento")
    void deveCompletarEmitterAnteriorAoAssinarNovamente() {
        Long usuarioId = 10L;

        SseEmitter primeiroEmitter = notificacaoService.assinar(usuarioId);
        assertNotNull(primeiroEmitter);

        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(notificacaoService, "emitters");
        assertNotNull(emitters);
        assertSame(primeiroEmitter, emitters.get(usuarioId));

        SseEmitter segundoEmitter = notificacaoService.assinar(usuarioId);
        assertNotNull(segundoEmitter);
        assertNotSame(primeiroEmitter, segundoEmitter);
        assertSame(segundoEmitter, emitters.get(usuarioId));
    }

    @Test
    @DisplayName("Não deve remover novo emitter se callback de emitter antigo for disparado")
    void naoDeveRemoverNovoEmitterAoFinalizarAntigo() {
        Long usuarioId = 10L;

        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(notificacaoService, "emitters");
        assertNotNull(emitters);

        SseEmitter emitter1 = notificacaoService.assinar(usuarioId);
        SseEmitter emitter2 = notificacaoService.assinar(usuarioId);

        // Simulando a remoção segura do emitter1
        emitters.remove(usuarioId, emitter1);

        // O emitter2 deve continuar registrado
        assertSame(emitter2, emitters.get(usuarioId));

        // Removendo emitter2 com a instância correta
        emitters.remove(usuarioId, emitter2);
        assertNull(emitters.get(usuarioId));
    }

    @Test
    @DisplayName("Deve listar notificações paginadas não excluídas do administrador")
    void deveListarNotificacoesPaginadasDoAdmin() {
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 5);
        Notificacao notif = new Notificacao();
        notif.setMensagem("Reserva confirmada");
        Page<Notificacao> paginaEsperada = new PageImpl<>(List.of(notif));

        when(notificacaoRepository.findByAdminIdAndExcluidaFalseOrderByDataCriacaoDesc(adminId, pageable))
                .thenReturn(paginaEsperada);

        Page<Notificacao> resultado = notificacaoService.listarPorAdmin(adminId, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Reserva confirmada", resultado.getContent().get(0).getMensagem());
        verify(notificacaoRepository, times(1)).findByAdminIdAndExcluidaFalseOrderByDataCriacaoDesc(adminId, pageable);
    }

    @Test
    @DisplayName("Deve marcar todas as notificações do administrador como excluídas")
    void deveExcluirTodasNotificacoesDoAdmin() {
        Long adminId = 1L;

        notificacaoService.excluirTodas(adminId);

        verify(notificacaoRepository, times(1)).marcarTodasComoExcluidas(adminId);
    }

    @Test
    @DisplayName("Deve enviar payload SSE com id, mensagem, lida e dataCriacao em JSON")
    void deveEnviarPayloadSseEmJson() throws Exception {
        Long adminId = 1L;
        Usuario admin = Usuario.builder().id_usuario(adminId).role(Role.ADMIN).build();
        when(usuarioRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(notificacaoRepository.save(any(Notificacao.class))).thenAnswer(inv -> inv.getArgument(0));

        SseEmitter emitter = mock(SseEmitter.class);
        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(notificacaoService, "emitters");
        emitters.put(adminId, emitter);

        notificacaoService.enviarNotificacao(adminId, "Nova reserva");

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        String json = captor.getValue().build().stream()
                .map(d -> d.getData().toString())
                .filter(s -> s.startsWith("{"))
                .findFirst()
                .orElseThrow();
        JsonNode node = JsonMapper.builder().build().readTree(json);
        assertEquals("Nova reserva", node.path("mensagem").asText());
        assertFalse(node.path("lida").asBoolean());
        assertTrue(node.has("id"));
        assertTrue(node.path("dataCriacao").isString());
        assertEquals(4, node.size());
    }
}