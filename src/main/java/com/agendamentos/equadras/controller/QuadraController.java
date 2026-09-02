package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.service.QuadraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quadras")
public class QuadraController {

    private final QuadraService quadraService;

    public QuadraController(QuadraService quadraService) {
        this.quadraService = quadraService;
    }

    @PostMapping
    public ResponseEntity<QuadraResponseDTO> cadastrar(
            @RequestBody @Valid QuadraCriacaoDTO dto,
            @RequestHeader(value = "X-Usuario-Id", required = true) Long adminId) {
        QuadraResponseDTO quadraCriada = quadraService.cadastrar(dto, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(quadraCriada);
    }

    @GetMapping
    public ResponseEntity<List<QuadraResponseDTO>> listarTodas(
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "2.0") Double raioKm) {
        return ResponseEntity.ok(quadraService.listar(usuarioId, latitude, longitude, raioKm));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuadraResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(quadraService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuadraResponseDTO> editar(
            @PathVariable Long id,
            @RequestBody @Valid QuadraCriacaoDTO dto,
            @RequestHeader(value = "X-Usuario-Id", required = true) Long adminId) {
        return ResponseEntity.ok(quadraService.editar(id, dto, adminId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = true) Long adminId) {
        quadraService.excluir(id, adminId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<QuadraResponseDTO> alternarStatus(
            @PathVariable Long id,
            @RequestParam boolean ativa,
            @RequestHeader(value = "X-Usuario-Id", required = true) Long adminId) {
        return ResponseEntity.ok(quadraService.alternarStatus(id, ativa, adminId));
    }

    @PostMapping(value = "/{id}/fotos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuadraResponseDTO> uploadFotos(
            @PathVariable Long id,
            @RequestParam("fotos") List<org.springframework.web.multipart.MultipartFile> fotos,
            @RequestHeader(value = "X-Usuario-Id", required = true) Long adminId) {
        return ResponseEntity.ok(quadraService.uploadFotos(id, fotos, adminId));
    }

    @DeleteMapping("/{id}/fotos")
    public ResponseEntity<QuadraResponseDTO> removerFoto(
            @PathVariable Long id,
            @RequestParam("fotoUrl") String fotoUrl,
            @RequestHeader(value = "X-Usuario-Id", required = true) Long adminId) {
        return ResponseEntity.ok(quadraService.removerFoto(id, fotoUrl, adminId));
    }
}