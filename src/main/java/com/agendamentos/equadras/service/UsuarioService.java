package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.LoginResponseDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponseDTO cadastrar(UsuarioCriacaoDTO dto) {
        if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        Usuario usuario = Usuario.builder()
                .nome_usuario(dto.nome_usuario())
                .email_usuario(dto.email_usuario())
                .senha_usuario(passwordEncoder.encode(dto.senha_usuario()))
                .phone_usuario(dto.phone_usuario())
                .role(Role.CLIENT)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        String token = jwtService.gerarToken(usuarioSalvo);
        return new LoginResponseDTO(token, UsuarioResponseDTO.fromEntity(usuarioSalvo));
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(UsuarioLoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail_usuario(dto.email_usuario())
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha incorretos."));

        if (!passwordEncoder.matches(dto.senha_usuario(), usuario.getSenha_usuario())) {
            throw new IllegalArgumentException("E-mail ou senha incorretos.");
        }

        String token = jwtService.gerarToken(usuario);
        return new LoginResponseDTO(token, UsuarioResponseDTO.fromEntity(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para o ID: " + id));
        return UsuarioResponseDTO.fromEntity(usuario);
    }
}