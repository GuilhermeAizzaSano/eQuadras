package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.dto.response.ApiKeyInfoDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.exception.RegraNegocioException;
import com.agendamentos.equadras.security.ApiKeyRateLimiter;
import com.agendamentos.equadras.security.ApiKeyService;
import com.agendamentos.equadras.security.JwtService;
import com.agendamentos.equadras.security.LoginRateLimiter;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.security.UsuarioLogadoArgumentResolver;
import com.agendamentos.equadras.service.AuditoriaService;
import com.agendamentos.equadras.service.UsuarioAuthService;
import com.agendamentos.equadras.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.agendamentos.equadras.shared.pagination.PageResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.util.List;

@Tag(name = "Usuários e Autenticação", description = "Endpoints para cadastro de atletas/admins, login com emissão de sessão HttpOnly, gerenciamento de perfil e chaves de API.")
@RestController
@RequestMapping({"/usuarios", "/api/usuarios"})
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioAuthService usuarioAuthService;
    private final JwtService jwtService;
    private final ApiKeyService apiKeyService;
    private final ApiKeyRateLimiter apiKeyRateLimiter;
    private final LoginRateLimiter loginRateLimiter;
    private final AuditoriaService auditoriaService;

    @Value("${equadras.cookie.secure:true}")
    private boolean cookieSecure;

    public UsuarioController(
            UsuarioService usuarioService,
            UsuarioAuthService usuarioAuthService,
            JwtService jwtService,
            ApiKeyService apiKeyService,
            ApiKeyRateLimiter apiKeyRateLimiter,
            LoginRateLimiter loginRateLimiter,
            AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.usuarioAuthService = usuarioAuthService;
        this.jwtService = jwtService;
        this.apiKeyService = apiKeyService;
        this.apiKeyRateLimiter = apiKeyRateLimiter;
        this.loginRateLimiter = loginRateLimiter;
        this.auditoriaService = auditoriaService;
    }

    private ResponseCookie criarCookieSessao(String token) {
        return ResponseCookie.from("equadras_session", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(jwtService.getExpiracaoMs()))
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie limparCookieSessao() {
        return ResponseCookie.from("equadras_session", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    @Operation(summary = "Realizar login",
            description = "Rota pública. Autentica por e-mail e senha, grava o JWT no cookie HttpOnly `equadras_session` (header `Set-Cookie`) e devolve no corpo apenas o perfil do usuário (`UsuarioResponseDTO`); o token NÃO vem no corpo. Efeito colateral: falhas consecutivas por e-mail (limite de 5) bloqueiam novas tentativas com 429 e `Retry-After`; login bem-sucedido zera o contador.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UsuarioLoginDTO dto) {
        String email = dto.email_usuario();
        if (loginRateLimiter.isEmailBloqueado(email)) {
            long retryAfter = loginRateLimiter.getRetryAfterSegundos(email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfter))
                    .body(org.springframework.http.ProblemDetail.forStatusAndDetail(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Muitas tentativas falhas de login para esta conta. Tente novamente em " + retryAfter + " segundos."
                    ));
        }

        try {
            var resposta = usuarioAuthService.login(dto);
            loginRateLimiter.registrarSucesso(email);
            ResponseCookie cookie = criarCookieSessao(resposta.token());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(resposta.usuario());
        } catch (RegraNegocioException e) {
            loginRateLimiter.registrarFalha(email);
            throw e;
        }
    }

    @Operation(summary = "Realizar logout",
            description = "Rota pública (funciona sem sessão). Se houver sessão, revoga os tokens do usuário, registra `LOGOUT` na auditoria e expira o cookie `equadras_session`. Responde 204 sem corpo, sempre.")
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        UsuarioAutenticado usuarioLogado = UsuarioLogadoArgumentResolver.usuarioAtualOuNulo();
        if (usuarioLogado != null && usuarioLogado.id() != null) {
            usuarioAuthService.revogarSessao(usuarioLogado.id());
            auditoriaService.registrarAcaoPorUsuarioId(usuarioLogado.id(), CategoriaAuditoria.AUTENTICACAO,
                    "LOGOUT", "USUARIO", usuarioLogado.id().toString(), "Logout efetuado com sucesso.");
        }
        ResponseCookie cookie = limparCookieSessao();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @Operation(summary = "Dados da minha sessão",
            description = "Papéis: CLIENT ou ADMIN. Devolve o perfil do usuário autenticado (cookie de sessão ou API-Key `eq_...`, enviada em `X-API-KEY` ou em `Authorization: Bearer eq_...`).")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(usuarioService.buscarPorId(usuarioLogado.id(), usuarioLogado.id()));
    }

    @Operation(summary = "Consultar metadados da minha API-KEY",
            description = "Papéis: CLIENT ou ADMIN. Informa se a conta possui chave, os 4 últimos caracteres e as datas de criação e último uso. Nunca devolve a chave em texto plano.")
    @GetMapping("/api-key")
    public ResponseEntity<ApiKeyInfoDTO> obterApiKeyInfo(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(apiKeyService.info(usuarioLogado.id()));
    }

    @Operation(summary = "Gerar ou regenerar API-KEY",
            description = "Papéis: CLIENT ou ADMIN. Emite uma chave opaca `eq_...`, invalidando a anterior imediatamente. A chave em texto plano só aparece nesta resposta (`Cache-Control: no-store`). Registra IP e User-Agent. Limite: 5 regenerações por minuto por conta (429 com `Retry-After: 60`).")
    @PostMapping("/api-key/regenerar")
    public ResponseEntity<?> regenerarApiKey(
            @UsuarioLogado UsuarioAutenticado usuarioLogado,
            HttpServletRequest request) {

        if (!apiKeyRateLimiter.tentarRegenerar(usuarioLogado.id())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(java.util.Map.of(
                            "status", 429,
                            "title", "Too Many Requests",
                            "detail", "Limite de 5 regenerações por minuto atingido para esta conta. Aguarde antes de tentar novamente."
                    ));
        }

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        ApiKeyCriadaDTO criada = apiKeyService.gerarOuRegenerar(usuarioLogado.id(), ip, userAgent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .body(criada);
    }

    @Operation(summary = "Revogar API-KEY",
            description = "Papéis: CLIENT ou ADMIN. Invalida a chave da conta sem gerar substituta. Idempotente: sem chave ativa também responde 204. Registra IP e User-Agent.")
    @DeleteMapping("/api-key")
    public ResponseEntity<Void> revogarApiKey(
            @UsuarioLogado UsuarioAutenticado usuarioLogado,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        apiKeyService.revogar(usuarioLogado.id(), ip, userAgent);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cadastrar novo usuário (Master Admin)",
            description = "Papel: ADMIN, e o service exige que seja o Master Admin (outro ADMIN recebe 403). Cria conta CLIENT ou ADMIN (`role` opcional). E-mail duplicado devolve 400 `EMAIL_DUPLICADO`; telefone repetido devolve 409 `TELEFONE_EM_USO`.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@RequestBody @Valid UsuarioCriacaoDTO dto,
                                                        @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        // cadastrarPorAdmin valida que o chamador é o Master Admin (403 para qualquer outro ADMIN)
        var resposta = usuarioService.cadastrarPorAdmin(dto, usuarioLogado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @Operation(summary = "Editar usuário existente (Master Admin)",
            description = "Papel: ADMIN, exigindo Master Admin. Atualiza nome, e-mail, telefone, `role` e, se `nova_senha` vier, a senha. O e-mail do Master Admin não pode ser alterado (400 `OPERACAO_NAO_PERMITIDA`).")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> editar(@Parameter(description = "ID do usuário (`id_usuario`)", example = "10") @PathVariable Long id,
                                                      @RequestBody @Valid com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO dto,
                                                      @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        var resposta = usuarioService.editarUsuario(id, dto, usuarioLogado.id());
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Excluir usuário (Master Admin)",
            description = "Papel: ADMIN, exigindo Master Admin. Remove o usuário. A conta do Master Admin não pode ser excluída (400 `OPERACAO_NAO_PERMITIDA`).")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID do usuário (`id_usuario`)", example = "10") @PathVariable Long id,
                                         @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.excluirUsuario(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar usuários (Master Admin)",
            description = """
                Papel: ADMIN, exigindo Master Admin. Duas variantes, escolhidas pela presença de `page`.

                **Com `page` (recomendada):** `PageResponse<UsuarioResponseDTO>`, com `size` padrão 10 e máximo 50. A ordenação é fixa por `id_usuario` crescente e o `sort` enviado é ignorado.

                **Sem `page` (legada, prefira sempre `page`):** lista simples `array<UsuarioResponseDTO>` de até 200 usuários, limite aplicado no SQL e sem aviso ao cliente. Nesta variante `size` e `sort` são ignorados.""")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(params = "page")
    public ResponseEntity<PageResponse<UsuarioResponseDTO>> listarUsuariosPaginado(@ParameterObject Pageable pageable,
                                                                          @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());
        return ResponseEntity.ok(usuarioService.listarPaginado(pageable));
    }

    @Operation(summary = "Listar usuários (Master Admin)",
            description = """
                Papel: ADMIN, exigindo Master Admin. Duas variantes, escolhidas pela presença de `page`.

                **Com `page` (recomendada):** `PageResponse<UsuarioResponseDTO>`, com `size` padrão 10 e máximo 50. A ordenação é fixa por `id_usuario` crescente e o `sort` enviado é ignorado.

                **Sem `page` (legada, prefira sempre `page`):** lista simples `array<UsuarioResponseDTO>` de até 200 usuários, limite aplicado no SQL e sem aviso ao cliente. Nesta variante `size` e `sort` são ignorados.""")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(params = "!page")
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(summary = "Alterar minha senha",
            description = "Papéis: CLIENT ou ADMIN. Exige a senha atual; a nova precisa ter 6+ caracteres com maiúscula, minúscula, número e símbolo e ser diferente da atual. Responde 204 sem corpo.")
    @PatchMapping("/minha-senha")
    public ResponseEntity<Void> alterarMinhaSenha(@RequestBody @Valid com.agendamentos.equadras.dto.request.AlterarSenhaDTO dto,
                                                  @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioAuthService.alterarMinhaSenha(usuarioLogado.id(), dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar usuário por ID",
            description = "Papéis: CLIENT ou ADMIN. O próprio usuário ou o Master Admin. Outro usuário recebe 403; ID inexistente, 404 `USUARIO_NAO_ENCONTRADO`.")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@Parameter(description = "ID do usuário (`id_usuario`)", example = "10") @PathVariable Long id,
                                                          @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id, usuarioLogado.id()));
    }
}