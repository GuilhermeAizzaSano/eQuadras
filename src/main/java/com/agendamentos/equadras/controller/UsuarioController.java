package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.LoginResponseDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<LoginResponseDTO> cadastrar(@RequestBody @Valid UsuarioCriacaoDTO dto) {
        var resposta = usuarioService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid UsuarioLoginDTO dto) {
        var resposta = usuarioService.login(dto);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos(@com.agendamentos.equadras.security.UsuarioLogado
                                                                  com.agendamentos.equadras.security.UsuarioAutenticado usuarioLogado) {
        if (usuarioLogado.role() != com.agendamentos.equadras.model.enums.Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas administradores podem listar usuários.");
        }
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }
}