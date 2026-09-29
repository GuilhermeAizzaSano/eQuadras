package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.response.EstatisticasAuditoriaDTO;
import com.agendamentos.equadras.dto.response.LogAuditoriaResponseDTO;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.AuditoriaService;
import com.agendamentos.equadras.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(summary = "Listar logs de auditoria com filtros",
            description = "Papel: ADMIN, exigindo Master Admin (outro ADMIN recebe 403). Página ordenada por `criadoEm` decrescente (ordenação fixa). Todos os filtros são opcionais e combináveis.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<LogAuditoriaResponseDTO>> listarLogs(
            @UsuarioLogado UsuarioAutenticado usuarioLogado,
            @Parameter(description = "Índice da página, começando em 0.", example = "0", schema = @Schema(defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página. Padrão 10; mínimo 1; máximo 100 (fora da faixa é ajustado).", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filtra pelo ID do usuário que executou a ação.", example = "10")
            @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Filtra pela categoria do evento (enum `CategoriaAuditoria`: AUTENTICACAO, AGENDAMENTO, QUADRA, USUARIO, BLOQUEIO, API_KEY).", example = "AUTENTICACAO")
            @RequestParam(required = false) CategoriaAuditoria categoria,
            @Parameter(description = "Filtra pelo código da ação (ex.: LOGOUT).", example = "LOGOUT")
            @RequestParam(required = false) String acao,
            @Parameter(description = "Início do período (ISO 8601 com fuso, ex.: `2026-09-29T00:00:00Z`), inclusivo.", example = "2026-09-29T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @Parameter(description = "Fim do período (ISO 8601 com fuso).", example = "2026-09-30T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @Parameter(description = "Busca livre de texto nos logs.", example = "arthur")
            @RequestParam(required = false) String busca) {

        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());

        int limitSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, limitSize, Sort.by(Sort.Direction.DESC, "criadoEm"));

        Page<LogAuditoriaResponseDTO> resultado = auditoriaService.listarLogs(
                pageable, usuarioId, categoria, acao, dataInicio, dataFim, busca
        );

        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Obter estatísticas de auditoria de hoje",
            description = "Papel: ADMIN, exigindo Master Admin. Contadores de hoje: logins, falhas de login, ações gerais e cancelamentos.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/estatisticas")
    public ResponseEntity<EstatisticasAuditoriaDTO> obterEstatisticas(
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {

        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());

        return ResponseEntity.ok(auditoriaService.obterEstatisticas());
    }
}
