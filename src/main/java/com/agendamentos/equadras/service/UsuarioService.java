package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.LoginResponseDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.JwtService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    public static final String MASTER_ADMIN_EMAIL = "gui@gmail.com";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public boolean isMasterAdmin(Long usuarioId) {
        if (usuarioId == null) return false;
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::isMasterAdmin)
                .orElse(false);
    }

    public void validarAcessoMasterAdmin(Long usuarioLogadoId) {
        if (!isMasterAdmin(usuarioLogadoId)) {
            throw new AccessDeniedException("Acesso restrito ao Administrador Geral do sistema.");
        }
    }

    @Transactional
    public UsuarioResponseDTO cadastrarPorAdmin(UsuarioCriacaoDTO dto, Long usuarioLogadoId) {
        validarAcessoMasterAdmin(usuarioLogadoId);

        if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        Role roleParaAtribuir = dto.role() != null ? dto.role() : Role.CLIENT;

        Usuario usuario = Usuario.builder()
                .nome_usuario(dto.nome_usuario())
                .email_usuario(dto.email_usuario())
                .senha_usuario(passwordEncoder.encode(dto.senha_usuario()))
                .phone_usuario(dto.phone_usuario())
                .role(roleParaAtribuir)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return UsuarioResponseDTO.fromEntity(usuarioSalvo);
    }

    @Transactional
    public LoginResponseDTO cadastrar(UsuarioCriacaoDTO dto) {
        if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        Role roleParaAtribuir = dto.role() != null ? dto.role() : Role.CLIENT;

        Usuario usuario = Usuario.builder()
                .nome_usuario(dto.nome_usuario())
                .email_usuario(dto.email_usuario())
                .senha_usuario(passwordEncoder.encode(dto.senha_usuario()))
                .phone_usuario(dto.phone_usuario())
                .role(roleParaAtribuir)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        String token = jwtService.gerarToken(usuarioSalvo);
        return new LoginResponseDTO(token, UsuarioResponseDTO.fromEntity(usuarioSalvo));
    }

    @Transactional
    public UsuarioResponseDTO editarUsuario(Long id, UsuarioEdicaoDTO dto, Long usuarioLogadoId) {
        validarAcessoMasterAdmin(usuarioLogadoId);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para o ID: " + id));

        // Se o e-mail foi alterado, verificar duplicidade
        if (!usuario.getEmail_usuario().equalsIgnoreCase(dto.email_usuario())) {
            if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
                throw new IllegalArgumentException("E-mail já cadastrado por outro usuário.");
            }
            // Não permitir alterar o e-mail do Admin Geral
            if (MASTER_ADMIN_EMAIL.equalsIgnoreCase(usuario.getEmail_usuario())) {
                throw new IllegalArgumentException("O e-mail do Administrador Geral não pode ser modificado.");
            }
            usuario.setEmail_usuario(dto.email_usuario());
        }

        usuario.setNome_usuario(dto.nome_usuario());
        usuario.setPhone_usuario(dto.phone_usuario());

        // Se for o Admin Geral, manter sempre como ADMIN
        if (MASTER_ADMIN_EMAIL.equalsIgnoreCase(usuario.getEmail_usuario())) {
            usuario.setRole(Role.ADMIN);
        } else if (dto.role() != null) {
            usuario.setRole(dto.role());
        }

        if (dto.nova_senha() != null && !dto.nova_senha().isBlank()) {
            usuario.setSenha_usuario(passwordEncoder.encode(dto.nova_senha()));
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        return UsuarioResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public void excluirUsuario(Long id, Long usuarioLogadoId) {
        validarAcessoMasterAdmin(usuarioLogadoId);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para o ID: " + id));

        if (MASTER_ADMIN_EMAIL.equalsIgnoreCase(usuario.getEmail_usuario())) {
            throw new IllegalArgumentException("A conta do Administrador Geral não pode ser excluída.");
        }

        usuarioRepository.delete(usuario);
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

    @Transactional
    public void alterarMinhaSenha(Long usuarioId, com.agendamentos.equadras.dto.request.AlterarSenhaDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha_usuario())) {
            throw new IllegalArgumentException("A senha atual informada está incorreta.");
        }

        if (dto.senhaAtual().equals(dto.novaSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenha_usuario(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario obterOuCriarUsuarioBot(String nome, String telefone) {
        String telefoneSanitizado = telefone != null ? telefone.replaceAll("\\D", "") : "";
        
        return usuarioRepository.findByPhone_usuario(telefoneSanitizado)
                .orElseGet(() -> {
                    try {
                        Usuario novoUsuario = Usuario.builder()
                                .nome_usuario(nome != null ? nome : "Usuário Bot")
                                .phone_usuario(telefoneSanitizado)
                                .email_usuario("bot_" + telefoneSanitizado + "@equadras.com")
                                .senha_usuario(passwordEncoder.encode("senhaBot123!"))
                                .role(Role.CLIENT)
                                .build();
                        return usuarioRepository.saveAndFlush(novoUsuario);
                    } catch (Exception e) {
                        // Concurrency issue fallback: someone just created the user
                        return usuarioRepository.findByPhone_usuario(telefoneSanitizado)
                                .orElseThrow(() -> new RuntimeException("Erro ao obter/criar usuário do bot: " + e.getMessage()));
                    }
                });
    }
}