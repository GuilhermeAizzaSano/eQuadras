package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.security.UsuarioLogadoArgumentResolver;
import com.agendamentos.equadras.service.QuadraBuscaService;
import com.agendamentos.equadras.service.QuadraFotoService;
import com.agendamentos.equadras.service.QuadraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Tag(name = "Quadras Esportivas", description = "Endpoints para consulta pública, busca por geolocalização e gestão de quadras e horários de funcionamento.")
@RestController
@RequestMapping({"/quadras", "/api/quadras"})
public class QuadraController {

    private final QuadraService quadraService;
    private final QuadraFotoService quadraFotoService;
    private final QuadraBuscaService quadraBuscaService;
    private final com.agendamentos.equadras.service.QuadraIdempotenciaService quadraIdempotenciaService;

    public QuadraController(QuadraService quadraService, QuadraFotoService quadraFotoService, QuadraBuscaService quadraBuscaService,
                            com.agendamentos.equadras.service.QuadraIdempotenciaService quadraIdempotenciaService) {
        this.quadraService = quadraService;
        this.quadraFotoService = quadraFotoService;
        this.quadraBuscaService = quadraBuscaService;
        this.quadraIdempotenciaService = quadraIdempotenciaService;
    }

    @Operation(
            summary = "Cadastrar nova quadra (Admin)",
            description = "Papel: ADMIN (a quadra passa a pertencer ao administrador autenticado). Cria a quadra com valor/hora, localização, `dataLimiteAgendamento` opcional (não pode estar no passado), até 5 URLs de foto e até 7 regras de disponibilidade semanal (um dia da semana só pode aparecer uma vez). Idempotente por `Idempotency-Key`: reenvio com a mesma chave em até 10 minutos devolve a quadra já criada em vez de duplicar. O campo `versao` do corpo é ignorado no POST."
    )
    @PostMapping
    public ResponseEntity<QuadraResponseDTO> cadastrar(@RequestBody @Valid QuadraCriacaoDTO dto,
                                                       @Parameter(description = "Chave gerada pelo cliente a cada formulário (ex.: UUID). Reenvios com a mesma chave em até 10 minutos devolvem a quadra já criada. Opcional: sem ela, cada chamada cria uma quadra.",
                                                               example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                       @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        QuadraResponseDTO quadraCriada = quadraIdempotenciaService.cadastrar(dto, usuarioLogado.id(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(quadraCriada);
    }

    @Operation(
            summary = "Listar quadras ativas, por proximidade e filtros",
            description = """
                Papéis: CLIENT ou ADMIN. Lista quadras ativas com filtros opcionais (texto parcial, sem diferenciar maiúsculas) e busca por proximidade (`latitude` + `longitude` + `raioKm`).

                **Formato da resposta** (decidido nesta ordem, a primeira regra que se aplica vence):
                1. `resumido=true|false` explícito;
                2. header `X-Client: frontend` ou `X-View: full` → completo;
                3. header `X-View: resumo|summary` ou `X-Client: api` → resumido;
                4. sem nada disso: URL com `/api/` → resumido (padrão de bots e integrações).

                **Variantes:** (a) resumido → `array<QuadraResumoResponseDTO>`; `page` e `size` são ignorados; (b) completo com `page` → `PageQuadraResponseDTO` (size padrão 6, máximo 50); (c) completo sem `page` → `array<QuadraResponseDTO>`.

                **Teto:** listas sem paginação (a e c) devolvem no máximo 200 itens, limite aplicado no SQL e sem aviso ao cliente: use `page` (formato completo) ou filtros para não perder resultados."""
    )
    @GetMapping
    public ResponseEntity<?> listarTodas(
            jakarta.servlet.http.HttpServletRequest request,
            @Parameter(description = "Latitude do ponto de busca por proximidade (graus, -90 a 90). Só filtra se `longitude` também vier.", example = "-20.8113")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "Longitude do ponto de busca por proximidade (graus, -180 a 180). Só filtra se `latitude` também vier.", example = "-49.3758")
            @RequestParam(required = false) Double longitude,
            @Parameter(description = "Raio da busca por proximidade, em km. Usado apenas com `latitude` e `longitude`.", example = "2.0",
                    schema = @Schema(defaultValue = "2.0"))
            @RequestParam(required = false, defaultValue = "2.0") Double raioKm,
            @Parameter(description = "Filtra pelo tipo de esporte. Valores: FUTEBOL, FUTSAL, VOLEI, BEACH_TENNIS, BASQUETE, TENIS.", example = "FUTEBOL")
            @RequestParam(required = false) String tipoEsporte,
            @Parameter(description = "Filtra por parte do nome da quadra.", example = "Arena")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtra por parte do endereço (logradouro).", example = "Av. Brasil")
            @RequestParam(required = false) String endereco,
            @Parameter(description = "Filtra pela cidade.", example = "São José do Rio Preto")
            @RequestParam(required = false) String cidade,
            @Parameter(description = "Filtra pelo bairro.", example = "Jardim das Flores")
            @RequestParam(required = false) String bairro,
            @Parameter(description = "Filtra pelo CEP.", example = "15000-000")
            @RequestParam(required = false) String cep,
            @Parameter(description = "Força o formato: `true` = resumido (`QuadraResumoResponseDTO`), `false` = completo (`QuadraResponseDTO`). Quando informado, tem precedência sobre os headers `X-Client`/`X-View` e sobre a URL.", example = "true")
            @RequestParam(required = false) Boolean resumido,
            @Parameter(description = "Índice da página, começando em 0. Só tem efeito no formato completo e o transforma em resposta paginada (`PageQuadraResponseDTO`). IGNORADO no formato resumido.", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Itens por página. Padrão 6; máximo 50 (valores maiores são limitados a 50; zero ou negativo volta para 6). Só tem efeito no formato completo com `page`. IGNORADO no formato resumido.",
                    example = "6", schema = @Schema(defaultValue = "6", maximum = "50"))
            @RequestParam(required = false, defaultValue = "6") Integer size,
            @Parameter(description = "Identifica o cliente. `frontend` força o formato completo; `api` força o resumido (só vale se `resumido` não vier).", example = "api",
                    schema = @Schema(allowableValues = {"frontend", "api"}))
            @RequestHeader(value = "X-Client", required = false) String client,
            @Parameter(description = "Formato desejado. `full` = completo; `resumo` ou `summary` = resumido (só vale se `resumido` não vier). Se `X-Client` também vier, `X-Client: frontend` e `X-View: full` são avaliados antes de `X-View: resumo|summary` e `X-Client: api`.", example = "resumo",
                    schema = @Schema(allowableValues = {"full", "resumo", "summary"}))
            @RequestHeader(value = "X-View", required = false) String view) {
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
            return ResponseEntity.ok(quadraBuscaService.listarResumido(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, endereco, cidade, bairro, cep));
        }

        // Rota /quadras (frontend) com paginação
        if (page != null) {
            int tamanhoPagina = (size != null && size > 0) ? Math.min(size, 50) : 6;
            Pageable pageable = PageRequest.of(page, tamanhoPagina);
            return ResponseEntity.ok(quadraBuscaService.listar(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, endereco, cidade, bairro, cep, pageable));
        }

        return ResponseEntity.ok(quadraBuscaService.listar(usuarioId, latitude, longitude, raioKm, tipoEsporte, nome, endereco, cidade, bairro, cep));
    }

    @Operation(summary = "Buscar quadra por ID",
            description = "Papéis: CLIENT ou ADMIN. Retorna a quadra completa, com fotos e disponibilidades semanais. ID inexistente devolve 400 (`IllegalArgumentException` no service, não 404).")
    @GetMapping("/{id}")
    public ResponseEntity<QuadraResponseDTO> buscarPorId(@Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(quadraService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar quadra (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Substitui os dados cadastrais e as disponibilidades. Controle de concorrência: o corpo deve trazer `versao` (a lida em `QuadraResponseDTO.versao`); ausente devolve 400 `VERSAO_OBRIGATORIA`; diferente da atual, 409 `CONFLITO_VERSAO`. `disponibilidades`: omitida ou `null` mantém os horários atuais; uma lista substitui todos; lista vazia (`[]`) apaga todos e a quadra fica fechada em todos os dias (o padrão 06:00–23:00 só vale no cadastro).")
    @PutMapping("/{id}")
    public ResponseEntity<QuadraResponseDTO> editar(@Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id,
                                                    @RequestBody @Valid QuadraCriacaoDTO dto,
                                                    @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraService.editar(id, dto, usuarioLogado.id()));
    }

    @Operation(summary = "Excluir quadra (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Remove a quadra somente se não houver agendamentos vinculados; caso contrário devolve 400 `OPERACAO_NAO_PERMITIDA` (recomendado: inativar via `PATCH /status`).")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        quadraService.excluir(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Alternar status da quadra (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Ativa (`ativa=true`) ou inativa (`ativa=false`) a quadra. Quadras inativas deixam de aparecer nas listagens e de aceitar novos agendamentos.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<QuadraResponseDTO> alternarStatus(@Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id,
                                                            @Parameter(description = "Novo estado: `true` ativa, `false` inativa. Obrigatório.", example = "false")
                                                            @RequestParam boolean ativa,
                                                            @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraService.alternarStatus(id, ativa, usuarioLogado.id()));
    }

    @Operation(summary = "Upload de fotos da quadra (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. `multipart/form-data` com uma ou mais partes `fotos` (JPEG, PNG ou WebP, até 5 MB cada). A galeria comporta no máximo 5 fotos: exceder devolve 400; arquivo maior que o limite, 413.")
    @PostMapping(value = "/{id}/fotos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuadraResponseDTO> uploadFotos(@Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id,
                                                         @Parameter(description = "Arquivos de imagem (parte multipart `fotos`, repetível).")
                                                         @RequestParam("fotos") List<org.springframework.web.multipart.MultipartFile> fotos,
                                                         @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraFotoService.uploadFotos(id, fotos, usuarioLogado.id()));
    }

    @Operation(summary = "Remover foto da quadra (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Remove a foto da galeria e o arquivo do disco. Devolve a quadra atualizada (200, com corpo).")
    @DeleteMapping("/{id}/fotos")
    public ResponseEntity<QuadraResponseDTO> removerFoto(@Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long id,
                                                         @Parameter(description = "URL da foto exatamente como consta em `QuadraResponseDTO.fotos`. Obrigatório.", example = "/uploads/quadras/3f2a9c1e-7b4d-4e8a-9c21-5d6f7e8a9b0c.jpg")
                                                         @RequestParam("fotoUrl") String fotoUrl,
                                                         @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(quadraFotoService.removerFoto(id, fotoUrl, usuarioLogado.id()));
    }

    @Operation(summary = "Consultar fotos da quadra",
            description = """
                Papéis: CLIENT ou ADMIN. Devolve a galeria por quadra. O formato depende dos parâmetros:
                - Com ID (path `/{id}/fotos`, ou `id`/`quadraId` na query; precedência: path, depois `id`, depois `quadraId`): um objeto `QuadraFotosResponseDTO`; ID inexistente devolve 400.
                - Sem ID e sem nenhum filtro de texto (`nome`, `tipoEsporte`, `cidade`, `bairro`): lista com TODAS as quadras (até 200).
                - Com filtro e exatamente uma correspondência: objeto único.
                - Com filtro e várias correspondências: lista.
                - Com filtro e nenhuma correspondência: `{"fotos": []}`.
                Aliases: `nomeQuadra` vale como `nome`, e `esporte` como `tipoEsporte`; se ambos vierem, o principal (`nome`/`tipoEsporte`) vence."""
    )
    @GetMapping({"/{id}/fotos", "/fotos"})
    public ResponseEntity<?> consultarFotos(
            @Parameter(description = "ID da quadra no path (rota `/{id}/fotos`). Tem precedência sobre `id` e `quadraId` da query.", example = "1")
            @PathVariable(name = "id", required = false) Long idPath,
            @Parameter(description = "ID da quadra na query. Usado se não houver ID no path.", example = "1")
            @RequestParam(name = "id", required = false) Long idParam,
            @Parameter(description = "Alias de `id`. Usado se não houver ID no path nem `id` na query.", example = "1")
            @RequestParam(required = false) Long quadraId,
            @Parameter(description = "Filtra por parte do nome da quadra.", example = "Arena")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Alias de `nome`. Só vale se `nome` estiver vazio.", example = "Arena")
            @RequestParam(required = false) String nomeQuadra,
            @Parameter(description = "Filtra pelo tipo de esporte (FUTEBOL, FUTSAL, VOLEI, BEACH_TENNIS, BASQUETE, TENIS).", example = "FUTEBOL")
            @RequestParam(required = false) String tipoEsporte,
            @Parameter(description = "Alias de `tipoEsporte`. Só vale se `tipoEsporte` estiver vazio.", example = "FUTEBOL")
            @RequestParam(required = false) String esporte,
            @Parameter(description = "Filtra pela cidade.", example = "São José do Rio Preto")
            @RequestParam(required = false) String cidade,
            @Parameter(description = "Filtra pelo bairro.", example = "Jardim das Flores")
            @RequestParam(required = false) String bairro) {
        Long idBuscado = idPath != null ? idPath : (idParam != null ? idParam : quadraId);
        String nomeBuscado = (nome != null && !nome.isBlank()) ? nome : nomeQuadra;
        String esporteBuscado = (tipoEsporte != null && !tipoEsporte.isBlank()) ? tipoEsporte : esporte;
        return ResponseEntity.ok(quadraFotoService.consultarFotos(idBuscado, nomeBuscado, esporteBuscado, cidade, bairro));
    }
}
