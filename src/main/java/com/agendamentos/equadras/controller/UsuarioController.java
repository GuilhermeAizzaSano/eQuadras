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

    @Operation(summary = "Realizar login", description = "Autentica via e-mail e senha, define o cookie HttpOnly de sessão e retorna o perfil do usuário.")
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

    @Operation(summary = "Realizar logout", description = "Encerra a sessão do usuário limpando o cookie HttpOnly e invalidando tokens ativos.")
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

    @Operation(summary = "Dados da minha sessão", description = "Retorna os dados do usuário autenticado pela sessão ativa (cookie HttpOnly).")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(usuarioService.buscarPorId(usuarioLogado.id(), usuarioLogado.id()));
    }

    @Operation(summary = "Consultar metadados da minha API-KEY", description = "Retorna informações sobre a existência, prefixo e datas de criação/último uso da chave de API da conta.")
    @GetMapping("/api-key")
    public ResponseEntity<ApiKeyInfoDTO> obterApiKeyInfo(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(apiKeyService.info(usuarioLogado.id()));
    }

    @Operation(summary = "Gerar ou regenerar API-KEY", description = "Emite uma nova chave de API opaca (eq_...) de alta entropia. O token anterior é invalidado de imediato. A chave em texto plano é devolvida exclusivamente nesta resposta.")
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

    @Operation(summary = "Revogar API-KEY", description = "Invalida imediatamente a chave de API da conta sem gerar uma substituta. É uma operação idempotente.")
    @DeleteMapping("/api-key")
    public ResponseEntity<Void> revogarApiKey(
            @UsuarioLogado UsuarioAutenticado usuarioLogado,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        apiKeyService.revogar(usuarioLogado.id(), ip, userAgent);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cadastrar novo usuário (Apenas Administrador)", description = "Cria uma nova conta de usuário (Role: CLIENT ou ADMIN). Requer sessão com privilégios de Administrador.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@RequestBody @Valid UsuarioCriacaoDTO dto,
                                                        @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        if (usuarioLogado != null && usuarioService.isMasterAdmin(usuarioLogado.id())) {
            var resposta = usuarioService.cadastrarPorAdmin(dto, usuarioLogado.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
        } else {
            var resposta = usuarioService.cadastrar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta.usuario());
        }
    }

    @Operation(summary = "Editar usuário existente (Apenas Admin Geral)", description = "Atualiza os dados de um usuário (nome, e-mail, telefone, perfil e opcionalmente senha). Apenas o Administrador Geral possui permissão.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> editar(@PathVariable Long id,
                                                      @RequestBody @Valid com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO dto,
                                                      @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        var resposta = usuarioService.editarUsuario(id, dto, usuarioLogado.id());
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Excluir usuário (Apenas Admin Geral)", description = "Remove um usuário do sistema. Apenas o Administrador Geral possui permissão.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id,
                                         @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.excluirUsuario(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar todos os usuários (Apenas Admin Geral)", description = "Retorna todos os usuários cadastrados no sistema (ADMIN e CLIENT). Apenas o Administrador Geral possui permissão.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(summary = "Alterar minha senha", description = "Permite que o próprio usuário autenticado por sessão altere sua senha informando a atual e a nova.")
    @PatchMapping("/minha-senha")
    public ResponseEntity<Void> alterarMinhaSenha(@RequestBody @Valid com.agendamentos.equadras.dto.request.AlterarSenhaDTO dto,
                                                  @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioAuthService.alterarMinhaSenha(usuarioLogado.id(), dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar usuário por ID", description = "Consulta os dados de um usuário pelo seu identificador único. Apenas o próprio usuário ou o Admin Geral tem permissão.")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id,
                                                          @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id, usuarioLogado.id()));
    }
}