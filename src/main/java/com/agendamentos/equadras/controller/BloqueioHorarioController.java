package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.BloqueioHorarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.BloqueioHorarioResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.BloqueioHorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bloqueios de Quadra", description = "Endpoints para gerenciamento de bloqueios pontuais (manutenções, feriados, horários indisponíveis) por quadra.")
@RestController
@RequestMapping({"/quadras", "/api/quadras"})
public class BloqueioHorarioController {

    private final BloqueioHorarioService bloqueioHorarioService;

    public BloqueioHorarioController(BloqueioHorarioService bloqueioHorarioService) {
        this.bloqueioHorarioService = bloqueioHorarioService;
    }

    @Operation(summary = "Criar bloqueio de horário ou dia inteiro (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Sem `horaInicio`/`horaFim` bloqueia o dia inteiro; com horário, ambos são obrigatórios e o início precisa ser anterior ao fim. A data não pode estar no passado. Falha com 400 se já houver reserva ativa no período ou bloqueio coincidente; se o dia já está totalmente bloqueado, a mensagem começa com `DIA_INTEIRO_BLOQUEADO` e `substituirDiaInteiro=true` desbloqueia o restante do dia mantendo só o novo intervalo.")
    @PostMapping("/{id}/bloqueios")
    public ResponseEntity<BloqueioHorarioResponseDTO> criarBloqueio(
            @Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id,
            @RequestBody @Valid BloqueioHorarioCriacaoDTO dto,
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        BloqueioHorarioResponseDTO bloqueio = bloqueioHorarioService.criarBloqueio(id, dto, usuarioLogado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueio);
    }

    @Operation(summary = "Listar bloqueios de todas as quadras do Admin (Admin)",
            description = "Papel: ADMIN. Reúne em uma chamada os bloqueios de todas as quadras do administrador autenticado.")
    @GetMapping("/bloqueios")
    public ResponseEntity<List<BloqueioHorarioResponseDTO>> listarTodosBloqueiosDoAdmin(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(bloqueioHorarioService.listarTodosDoAdmin(usuarioLogado.id()));
    }

    @Operation(summary = "Listar bloqueios da quadra",
            description = "Papéis: CLIENT ou ADMIN. Lista os bloqueios ativos e futuros de uma quadra. Quadra inexistente devolve 400.")
    @GetMapping("/{id}/bloqueios")
    public ResponseEntity<List<BloqueioHorarioResponseDTO>> listarBloqueios(@Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(bloqueioHorarioService.listarBloqueios(id));
    }

    @Operation(summary = "Remover bloqueio por ID (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Remove um bloqueio. O bloqueio precisa pertencer à quadra do path (senão 400). Responde 204 sem corpo.")
    @DeleteMapping("/{quadraId}/bloqueios/{bloqueioId}")
    public ResponseEntity<Void> removerBloqueio(
            @Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long quadraId,
            @Parameter(description = "ID do bloqueio (`id` de `BloqueioHorarioResponseDTO`)", example = "5") @PathVariable Long bloqueioId,
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        bloqueioHorarioService.removerBloqueio(quadraId, bloqueioId, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desbloquear horários ou dia (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Informe `bloqueioId` OU `data` (com `horaInicio`/`horaFim` opcionais para desbloquear só um intervalo). Sem nenhum dos dois, 400. Devolve `{mensagem, totalRemovidos}` com quantos bloqueios foram removidos.")
    @PostMapping("/{quadraId}/desbloquear")
    public ResponseEntity<java.util.Map<String, Object>> desbloquear(
            @Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long quadraId,
            @RequestBody @Valid com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO dto,
            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        int removidos = bloqueioHorarioService.desbloquearHorarios(quadraId, dto, usuarioLogado.id());
        return ResponseEntity.ok(java.util.Map.of(
                "mensagem", "Horários desbloqueados com sucesso.",
                "totalRemovidos", removidos
        ));
    }
}
