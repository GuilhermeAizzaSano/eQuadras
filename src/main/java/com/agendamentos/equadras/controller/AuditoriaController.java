package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.response.EstatisticasAuditoriaDTO;
import com.agendamentos.equadras.dto.response.LogAuditoriaResponseDTO;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.AuditoriaService;
import com.agendamentos.equadras.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Auditoria & Logs", description = "Endpoints exclusivos para o Administrador Geral inspecionar a trilha de auditoria do sistema.")
@RestController
@RequestMapping({"/admin/auditoria", "/api/admin/auditoria"})
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    public AuditoriaController(AuditoriaService auditoriaService, UsuarioService usuarioService) {
        this.auditoriaService = auditoriaService;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar logs de auditoria com filtros", description = "Retorna logs paginados e filtráveis. Apenas o Administrador Geral (gui@gmail.com) tem acesso.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<LogAuditoriaResponseDTO>> listarLogs(
            @UsuarioLogado UsuarioAutenticado usuarioLogado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) CategoriaAuditoria categoria,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) String busca) {

        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());

        int limitSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, limitSize, Sort.by(Sort.Direction.DESC, "criadoEm"));

        Page<LogAuditoriaResponseDTO> resultado = auditoriaService.listarLogs(
                pageable, usuarioId, categoria, acao, dataInicio, dataFim, busca
        );

        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Obter estatísticas de auditoria de hoje", description = "Retorna contadores de logins, falhas, cancelamentos e ações gerais do dia de hoje.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/estatisticas")
    public ResponseEntity<EstatisticasAuditoriaDTO> obterEstatisticas(
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {

        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());

        return ResponseEntity.ok(auditoriaService.obterEstatisticas());
    }
}
