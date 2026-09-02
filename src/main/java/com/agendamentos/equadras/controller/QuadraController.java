package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.security.UsuarioLogadoArgumentResolver;
import com.agendamentos.equadras.service.QuadraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quadras Esportivas", description = "Endpoints para consulta pública, busca por geolocalização e gestão de quadras e horários de funcionamento.")
@RestController
@RequestMapping("/quadras")
public class QuadraController {

    private final QuadraService quadraService;

    public QuadraController(QuadraService quadraService) {
        this.quadraService = quadraService;
    }

    @Operation(
            summary = "Cadastrar nova quadra (Admin)",
            description = "Cria uma nova quadra esportiva definindo nome, tipo de esporte, valor/hora, localização, data limite de agendamento (opcional), até 5 fotos e grade personalizada de horários por dia da semana (disponibilidades). Requer ROLE_ADMIN."
    )
    @PostMapping
    public ResponseEntity<QuadraResponseDTO> cadastrar(@RequestBody @Valid QuadraCriacaoDTO dto,
                                                       @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        QuadraResponseDTO quadraCriada = quadraService.cadastrar(dto, usuarioLogado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(quadraCriada);
    }

    @Operation(summary = "Listar quadras ativas / por proximidade", description = "Lista todas as quadras ativas. Permite filtrar por proximidade geográfica informando latitude, longitude e raio em KM.")
    @GetMapping
    public ResponseEntity<List<QuadraResponseDTO>> listarTodas(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "2.0") Double raioKm) {
        UsuarioAutenticado usuarioLogado = UsuarioLogadoArgumentResolver.usuarioAtualOuNulo();
        Long usuarioId = usuarioLogado != null ? usuarioLogado.id() : null;
        return ResponseEntity.ok(quadraService.listar(usuarioId, latitude, longitude, raioKm));
    }

    @Operation(summary = "Buscar quadra por ID", description = "Retorna os detalhes completos, fotos e horários de funcionamento de uma quadra específica.")
    @GetMapping("/{id}")
    public ResponseEntity<QuadraResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(quadraService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar quadra (Admin)", description = "Atualiza os dados cadastrais, endereço e horários de funcionamento da quadra do admin autenticado.")
    @PutMapping("/{id}")
    public ResponseEntity<QuadraResponseDTO> editar(@PathVariable Long id,
                                                    @RequestBody @Valid QuadraCriacaoDTO dto,
                                                    @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraService.editar(id, dto, usuarioLogado.id()));
    }

    @Operation(summary = "Excluir quadra (Admin)", description = "Remove a quadra do sistema caso ela não possua histórico de reservas.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        quadraService.excluir(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Alternar status da quadra (Admin)", description = "Ativa ou inativa a quadra para novos agendamentos.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<QuadraResponseDTO> alternarStatus(@PathVariable Long id,
                                                            @RequestParam boolean ativa,
                                                            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraService.alternarStatus(id, ativa, usuarioLogado.id()));
    }

    @Operation(summary = "Upload de fotos da quadra (Admin)", description = "Envia imagens (JPEG, PNG, WebP) de até 5MB para a galeria da quadra (máximo 5 fotos).")
    @PostMapping(value = "/{id}/fotos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuadraResponseDTO> uploadFotos(@PathVariable Long id,
                                                         @RequestParam("fotos") List<org.springframework.web.multipart.MultipartFile> fotos,
                                                         @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraService.uploadFotos(id, fotos, usuarioLogado.id()));
    }

    @Operation(summary = "Remover foto da quadra (Admin)", description = "Exclui uma foto específica da galeria e do armazenamento de disco.")
    @DeleteMapping("/{id}/fotos")
    public ResponseEntity<QuadraResponseDTO> removerFoto(@PathVariable Long id,
                                                         @RequestParam("fotoUrl") String fotoUrl,
                                                         @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraService.removerFoto(id, fotoUrl, usuarioLogado.id()));
    }
}