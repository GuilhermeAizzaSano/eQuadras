package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Agendamentos e Reservas", description = "Endpoints para agendamento concorrente com lock pessimista, verificação de slots e cancelamento de reservas.")
@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @Operation(summary = "Criar novo agendamento com Lock e Pix", description = "Bloqueia a quadra sob lock pessimista para evitar conflitos concorrentes e gera a cobrança Pix.")
    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> agendar(@RequestBody @Valid AgendamentoCriacaoDTO dto,
                                                          @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        AgendamentoResponseDTO resposta = agendamentoService.agendar(dto, usuarioLogado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @Operation(summary = "Listar agendamentos do usuário autenticado", description = "Retorna o histórico de todas as reservas realizadas pelo atleta logado ou pelas quadras do admin.")
    @GetMapping
    public ResponseEntity<List<AgendamentoResponseDTO>> listarTodos(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(agendamentoService.listarTodos(usuarioLogado.id()));
    }

    @Operation(summary = "Cancelar agendamento", description = "Cancela uma reserva ativa pertencente ao usuário autenticado ou ao administrador da quadra.")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(@PathVariable Long id,
                                                           @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(agendamentoService.cancelar(id, usuarioLogado.id()));
    }

    @Operation(summary = "Listar agendamentos por quadra e data", description = "Retorna as reservas cadastradas para uma quadra em um determinado dia.")
    @GetMapping("/quadra/{quadraId}/data")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarPorQuadraEData(
            @PathVariable Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(agendamentoService.listarPorQuadraEData(quadraId, data));
    }

    @Operation(summary = "Consultar horários dinâmicos disponíveis", description = "Gera a grade de slots de 1 hora para a quadra na data informada respeitando os horários configurados para o dia da semana.")
    @GetMapping("/quadra/{quadraId}/horarios-disponiveis")
    public ResponseEntity<List<HorarioDisponivelDTO>> listarHorariosDisponiveis(
            @PathVariable Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(agendamentoService.listarHorariosDisponiveis(quadraId, data));
    }
}