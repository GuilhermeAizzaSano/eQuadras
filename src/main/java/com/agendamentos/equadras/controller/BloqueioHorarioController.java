package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.BloqueioHorarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.BloqueioHorarioResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.BloqueioHorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bloqueios de Quadra", description = "Endpoints para gerenciamento de bloqueios pontuais (manutenções, feriados, horários indisponíveis) por quadra.")
@RestController
@RequestMapping("/quadras")
public class BloqueioHorarioController {

    private final BloqueioHorarioService bloqueioHorarioService;

    public BloqueioHorarioController(BloqueioHorarioService bloqueioHorarioService) {
        this.bloqueioHorarioService = bloqueioHorarioService;
    }

    @Operation(summary = "Criar bloqueio de horário/dia (Admin)", description = "Bloqueia um dia inteiro ou um intervalo de horários de uma quadra. Requer ROLE_ADMIN e ser dono da quadra.")
    @PostMapping("/{id}/bloqueios")
    public ResponseEntity<BloqueioHorarioResponseDTO> criarBloqueio(
            @PathVariable Long id,
            @RequestBody @Valid BloqueioHorarioCriacaoDTO dto,
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        BloqueioHorarioResponseDTO bloqueio = bloqueioHorarioService.criarBloqueio(id, dto, usuarioLogado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueio);
    }

    @Operation(summary = "Listar bloqueios da quadra", description = "Lista todos os bloqueios ativos e futuros da quadra.")
    @GetMapping("/{id}/bloqueios")
    public ResponseEntity<List<BloqueioHorarioResponseDTO>> listarBloqueios(@PathVariable Long id) {
        return ResponseEntity.ok(bloqueioHorarioService.listarBloqueios(id));
    }

    @Operation(summary = "Remover bloqueio por ID (Admin)", description = "Remove um bloqueio existente pelo seu ID. Requer ROLE_ADMIN e ser dono da quadra.")
    @DeleteMapping("/{quadraId}/bloqueios/{bloqueioId}")
    public ResponseEntity<Void> removerBloqueio(
            @PathVariable Long quadraId,
            @PathVariable Long bloqueioId,
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        bloqueioHorarioService.removerBloqueio(quadraId, bloqueioId, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desbloquear horários/dia da quadra (Admin)", description = "Desbloqueia horários ou dias de uma quadra informando o ID do bloqueio ou a data e horários no corpo da requisição. Requer ROLE_ADMIN e ser dono da quadra.")
    @PostMapping("/{quadraId}/desbloquear")
    public ResponseEntity<java.util.Map<String, Object>> desbloquear(
            @PathVariable Long quadraId,
            @RequestBody @Valid com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO dto,
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        int removidos = bloqueioHorarioService.desbloquearHorarios(quadraId, dto, usuarioLogado.id());
        return ResponseEntity.ok(java.util.Map.of(
                "mensagem", "Horários desbloqueados com sucesso.",
                "totalRemovidos", removidos
        ));
    }
}
