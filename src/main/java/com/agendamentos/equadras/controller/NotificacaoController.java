package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.Notificacao;
import com.agendamentos.equadras.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
@CrossOrigin("*")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping(value = "/stream/{adminId}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long adminId) {
        return notificacaoService.assinar(adminId);
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<List<Notificacao>> listarPorAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(notificacaoService.listarPorAdmin(adminId));
    }

    @PutMapping("/{id}/ler")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        notificacaoService.marcarComoLida(id);
        return ResponseEntity.noContent().build();
    }
}
