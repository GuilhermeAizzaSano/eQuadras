package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioCriacaoDTO dto) {
        if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        Role role = (dto.role() != null) ? dto.role() : Role.CLIENT;

        Usuario usuario = Usuario.builder()
                .nome_usuario(dto.nome_usuario())
                .email_usuario(dto.email_usuario())
                .senha_usuario(passwordEncoder.encode(dto.senha_usuario()))
                .phone_usuario(dto.phone_usuario())
                .role(role)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return UsuarioResponseDTO.fromEntity(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO login(UsuarioLoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail_usuario(dto.email_usuario())
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha incorretos."));

        if (!passwordEncoder.matches(dto.senha_usuario(), usuario.getSenha_usuario())) {
            throw new IllegalArgumentException("E-mail ou senha incorretos.");
        }

        return UsuarioResponseDTO.fromEntity(usuario);
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