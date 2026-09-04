package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.LoginResponseDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Usuários e Autenticação", description = "Endpoints para cadastro de atletas/admins, login com emissão de token JWT e consulta de perfis.")
@RestController
@RequestMapping({"/usuarios", "/api/usuarios"})
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Cadastrar novo usuário (Apenas Admin Geral ou Auto-cadastro)", description = "Cria uma nova conta de usuário (Role: CLIENT ou ADMIN). Se for anônimo, auto-cadastra como CLIENT.")
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody @Valid UsuarioCriacaoDTO dto) {
        UsuarioAutenticado usuarioLogado = com.agendamentos.equadras.security.UsuarioLogadoArgumentResolver.usuarioAtualOuNulo();
        if (usuarioLogado != null && usuarioService.isMasterAdmin(usuarioLogado.id())) {
            var resposta = usuarioService.cadastrarPorAdmin(dto, usuarioLogado.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
        } else {
            var resposta = usuarioService.cadastrar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
        }
    }

    @Operation(summary = "Editar usuário existente (Apenas Admin Geral)", description = "Atualiza os dados de um usuário (nome, e-mail, telefone, perfil e opcionalmente senha). Apenas o Administrador Geral possui permissão.")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> editar(@PathVariable Long id,
                                                      @RequestBody @Valid com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO dto,
                                                      @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        var resposta = usuarioService.editarUsuario(id, dto, usuarioLogado.id());
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Excluir usuário (Apenas Admin Geral)", description = "Remove um usuário do sistema. Apenas o Administrador Geral possui permissão.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id,
                                         @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.excluirUsuario(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Realizar login", description = "Autentica via e-mail e senha e retorna o token JWT assinado para autenticação nas demais rotas.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid UsuarioLoginDTO dto) {
        var resposta = usuarioService.login(dto);
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Listar todos os usuários (Apenas Admin Geral)", description = "Retorna todos os usuários cadastrados no sistema (ADMIN e CLIENT). Apenas o Administrador Geral possui permissão.")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.validarAcessoMasterAdmin(usuarioLogado.id());
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(summary = "Alterar minha senha", description = "Permite que o próprio usuário autenticado altere sua senha informando a atual e a nova (mínimo 6 chars, 1 maiúscula, 1 minúscula, 1 número e 1 símbolo).")
    @PatchMapping("/minha-senha")
    public ResponseEntity<Void> alterarMinhaSenha(@RequestBody @Valid com.agendamentos.equadras.dto.request.AlterarSenhaDTO dto,
                                                  @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        usuarioService.alterarMinhaSenha(usuarioLogado.id(), dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar usuário por ID", description = "Consulta os dados de um usuário pelo seu identificador único.")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }
}