package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> agendar(@RequestBody @Valid AgendamentoCriacaoDTO dto) {
        AgendamentoResponseDTO resposta = agendamentoService.agendar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponseDTO>> listarTodos(
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return ResponseEntity.ok(agendamentoService.listarTodos(usuarioId));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = true) Long usuarioId) {
        return ResponseEntity.ok(agendamentoService.cancelar(id, usuarioId));
    }

    @GetMapping("/quadra/{quadraId}/data")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarPorQuadraEData(
            @PathVariable Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(agendamentoService.listarPorQuadraEData(quadraId, data));
    }

    @GetMapping("/quadra/{quadraId}/horarios-disponiveis")
    public ResponseEntity<List<HorarioDisponivelDTO>> listarHorariosDisponiveis(
            @PathVariable Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(agendamentoService.listarHorariosDisponiveis(quadraId, data));
    }
}