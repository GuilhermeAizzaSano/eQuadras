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
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Cadastrar novo usuário", description = "Cria uma nova conta de usuário (Role: CLIENT) e retorna o perfil com o token JWT de autenticação.")
    @PostMapping
    public ResponseEntity<LoginResponseDTO> cadastrar(@RequestBody @Valid UsuarioCriacaoDTO dto) {
        var resposta = usuarioService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @Operation(summary = "Realizar login", description = "Autentica via e-mail e senha e retorna o token JWT assinado para autenticação nas demais rotas.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid UsuarioLoginDTO dto) {
        var resposta = usuarioService.login(dto);
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Listar todos os usuários (Admin)", description = "Retorna todos os usuários cadastrados no sistema. Requer permissão ROLE_ADMIN.")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        if (usuarioLogado.role() != com.agendamentos.equadras.model.enums.Role.ADMIN) {
            throw new AccessDeniedException("Apenas administradores podem listar usuários.");
        }
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(summary = "Buscar usuário por ID", description = "Consulta os dados públicos de um usuário pelo seu identificador único.")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }
}