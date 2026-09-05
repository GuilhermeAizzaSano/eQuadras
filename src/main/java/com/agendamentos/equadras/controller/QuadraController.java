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
@RequestMapping({"/quadras", "/api/quadras"})
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

    @Operation(
            summary = "Listar quadras ativas / por proximidade e filtros",
            description = "Lista todas as quadras ativas. Na rota /quadras (Frontend), retorna a lista completa com fotos e disponibilidades (QuadraResponseDTO). Na rota /api/quadras (Bot / Integrações), retorna o formato resumido (QuadraResumoResponseDTO). Também suporta o parâmetro 'resumido=true/false'."
    )
    @GetMapping
    public ResponseEntity<?> listarTodas(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "2.0") Double raioKm,
            @RequestParam(required = false) String tipoEsporte,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) String cep,
            @RequestParam(required = false) Boolean resumido,
            @RequestHeader(value = "X-Client", required = false) String client,
            @RequestHeader(value = "X-View", required = false) String view,
            @RequestHeader(value = "Origin", required = false) String origin) {
        UsuarioAutenticado usuarioLogado = UsuarioLogadoArgumentResolver.usuarioAtualOuNulo();
        Long usuarioId = usuarioLogado != null ? usuarioLogado.id() : null;

        String uri = request != null ? request.getRequestURI() : "";
        boolean isApiRoute = uri != null && uri.contains("/api/");

        boolean querResumido;
        if (resumido != null) {
            querResumido = resumido;
        } else if ("frontend".equalsIgnoreCase(client) || "full".equalsIgnoreCase(view)) {
            querResumido = false;
        } else if ("resumo".equalsIgnoreCase(view) || "summary".equalsIgnoreCase(view) || "api".equalsIgnoreCase(client)) {
            querResumido = true;
        } else {
            // Se chamado via /api/quadras -> padrão resumido (para bots e terceiros)
            // Se chamado via /quadras -> padrão completo (para frontend)
            querResumido = isApiRoute;
        }

        if (querResumido) {
            return ResponseEntity.ok(quadraService.listarResumido(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, cidade, bairro, cep));
        }

        return ResponseEntity.ok(quadraService.listar(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, cidade, bairro, cep));
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

    @Operation(summary = "Consultar fotos da quadra", description = "Retorna a galeria de fotos de uma quadra específica por ID, Nome, Tipo de Esporte, Cidade ou Bairro, ou lista todas as quadras com suas fotos se nenhum parâmetro for informado.")
    @GetMapping({"/{id}/fotos", "/fotos"})
    public ResponseEntity<?> consultarFotos(
            @PathVariable(name = "id", required = false) Long idPath,
            @RequestParam(name = "id", required = false) Long idParam,
            @RequestParam(required = false) Long quadraId,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String nomeQuadra,
            @RequestParam(required = false) String tipoEsporte,
            @RequestParam(required = false) String esporte,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String bairro) {
        Long idBuscado = idPath != null ? idPath : (idParam != null ? idParam : quadraId);
        if (idBuscado != null) {
            QuadraResponseDTO q = quadraService.buscarPorId(idBuscado);
            return ResponseEntity.ok(mapQuadraFotos(q));
        }

        String nomeBuscado = (nome != null && !nome.isBlank()) ? nome : nomeQuadra;
        String esporteBuscado = (tipoEsporte != null && !tipoEsporte.isBlank()) ? tipoEsporte : esporte;

        boolean temFiltro = (nomeBuscado != null && !nomeBuscado.isBlank())
                || (esporteBuscado != null && !esporteBuscado.isBlank())
                || (cidade != null && !cidade.isBlank())
                || (bairro != null && !bairro.isBlank());

        if (temFiltro) {
            java.util.List<com.agendamentos.equadras.model.entity.Quadra> quadras =
                    quadraService.filtrarQuadrasEntidades(null, null, null, null, esporteBuscado, nomeBuscado, cidade, bairro, null);
            if (quadras.size() == 1) {
                return ResponseEntity.ok(mapQuadraFotos(quadras.get(0)));
            } else if (!quadras.isEmpty()) {
                return ResponseEntity.ok(quadras.stream().map(this::mapQuadraFotos).toList());
            }
            return ResponseEntity.ok(java.util.Map.of("fotos", java.util.List.of()));
        }

        // Se não passou id, nome nem esporte, lista todas as quadras ativas com suas fotos organizadas por quadra
        java.util.List<java.util.Map<String, Object>> todas = quadraService.filtrarQuadrasEntidades(null, null, null, null, null, null, null, null, null)
                .stream()
                .map(this::mapQuadraFotos)
                .toList();

        return ResponseEntity.ok(todas);
    }

    private java.util.Map<String, Object> mapQuadraFotos(com.agendamentos.equadras.model.entity.Quadra q) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id_quadra", q.getId_quadra());
        map.put("nome", q.getNome());
        map.put("tipoEsporte", q.getTipoEsporte() != null ? q.getTipoEsporte().name() : null);
        map.put("cidade", q.getCidade() != null ? q.getCidade() : "");
        map.put("bairro", q.getBairro() != null ? q.getBairro() : "");
        map.put("fotos", q.getFotos() != null ? new java.util.ArrayList<>(q.getFotos()) : java.util.List.of());
        return map;
    }

    private java.util.Map<String, Object> mapQuadraFotos(QuadraResponseDTO q) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id_quadra", q.id_quadra());
        map.put("nome", q.nome());
        map.put("tipoEsporte", q.tipoEsporte() != null ? q.tipoEsporte().name() : null);
        map.put("cidade", q.cidade() != null ? q.cidade() : "");
        map.put("bairro", q.bairro() != null ? q.bairro() : "");
        map.put("fotos", q.fotos() != null ? new java.util.ArrayList<>(q.fotos()) : java.util.List.of());
        return map;
    }
}