package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.response.NotificacaoResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Tag(name = "Notificações", description = "Endpoints para consulta e marcação de notificações de administradores (Apenas Admin).")
@RestController
@RequestMapping({"/notificacoes", "/api/notificacoes"})
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Operation(summary = "Streaming SSE de notificações",
            description = "Papel: ADMIN. Abre uma conexão Server-Sent Events (`text/event-stream`, timeout de 1 hora, uma conexão por administrador: nova conexão substitui a anterior). Cada notificação chega como evento `notificacao` com `data` JSON `{id, mensagem, lida, dataCriacao}`. Os eventos são enviados após o commit da reserva ou pagamento que os originou.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return notificacaoService.assinar(usuarioLogado.id());
    }

    @Operation(summary = "Listar notificações do administrador",
            description = "Papel: ADMIN. Página (`Page`) das notificações de reservas e pagamentos do administrador autenticado, sem as excluídas. `size` é limitado a 50.")
    @GetMapping("/admin")
    public ResponseEntity<Page<NotificacaoResponseDTO>> listarPorAdmin(
            @UsuarioLogado UsuarioAutenticado usuarioLogado,
            @Parameter(description = "Índice da página, começando em 0.", example = "0", schema = @Schema(defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página. Padrão 5; máximo 50 (valores maiores são limitados a 50).", example = "5", schema = @Schema(defaultValue = "5", maximum = "50"))
            @RequestParam(defaultValue = "5") int size) {
        // Teto alinhado ao spring.data.web.pageable.max-page-size global
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(notificacaoService.listarPorAdmin(usuarioLogado.id(), pageable)
                .map(NotificacaoResponseDTO::fromEntity));
    }

    @Operation(summary = "Marcar notificação como lida",
            description = "Papel: ADMIN. Marca uma notificação do próprio administrador como lida. Notificação de outro usuário devolve 400. Responde 204 sem corpo.")
    @PutMapping("/{id}/ler")
    public ResponseEntity<Void> marcarComoLida(@Parameter(description = "ID da notificação", example = "1") @PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        notificacaoService.marcarComoLidaSeDoUsuario(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marcar todas as notificações como lidas",
            description = "Papel: ADMIN. Marca como lidas todas as notificações do administrador autenticado. Responde 204 sem corpo.")
    @PutMapping("/ler-todas")
    public ResponseEntity<Void> marcarTodasComoLidas(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        notificacaoService.marcarTodasComoLidas(usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Excluir todas as notificações",
            description = "Papel: ADMIN. Exclusão lógica (soft delete) de todas as notificações do administrador autenticado; elas deixam de aparecer na listagem. Responde 204 sem corpo.")
    @DeleteMapping("/todas")
    public ResponseEntity<Void> excluirTodas(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        notificacaoService.excluirTodas(usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }
}
