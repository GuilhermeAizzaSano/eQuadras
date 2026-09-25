package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO;
import com.agendamentos.equadras.dto.response.LoginResponseDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.exception.RecursoNaoEncontradoException;
import com.agendamentos.equadras.exception.RegraNegocioException;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.JwtService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;
    private final String masterAdminEmail;
    private final String botDefaultPassword;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditoriaService auditoriaService,
            @org.springframework.beans.factory.annotation.Value("${admin.master.email:gui@gmail.com}") String masterAdminEmail,
            @org.springframework.beans.factory.annotation.Value("${equadras.bot.default-password:}") String botDefaultPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
        this.masterAdminEmail = masterAdminEmail != null ? masterAdminEmail.trim().toLowerCase() : "gui@gmail.com";
        this.botDefaultPassword = botDefaultPassword != null ? botDefaultPassword.trim() : "";

        // Sincroniza a propriedade estática de fallback na entidade
        Usuario.MASTER_EMAIL_CONFIGURADO = this.masterAdminEmail;
    }

    public String getMasterAdminEmail() {
        return masterAdminEmail;
    }

    public boolean isMasterAdmin(Long usuarioId) {
        if (usuarioId == null) return false;
        return usuarioRepository.findById(usuarioId)
                .map(u -> u.isMasterAdmin(this.masterAdminEmail))
                .orElse(false);
    }

    public boolean podeGerenciarQuadra(Quadra quadra, Long adminId) {
        if (adminId == null) return false;
        if (isMasterAdmin(adminId)) return true;
        return quadra.getAdmin() != null && adminId.equals(quadra.getAdmin().getId_usuario());
    }

    public void validarAcessoMasterAdmin(Long usuarioLogadoId) {
        if (!isMasterAdmin(usuarioLogadoId)) {
            throw new AccessDeniedException("Acesso restrito ao Administrador Geral do sistema.");
        }
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Usuario> buscarPorIdEntidade(Long id) {
        if (id == null) return java.util.Optional.empty();
        return usuarioRepository.findById(id);
    }

    @Transactional
    public UsuarioResponseDTO cadastrarPorAdmin(UsuarioCriacaoDTO dto, Long usuarioLogadoId) {
        validarAcessoMasterAdmin(usuarioLogadoId);

        if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
            throw new RegraNegocioException("EMAIL_DUPLICADO", "E-mail já cadastrado no sistema.");
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
        if (auditoriaService != null) {
            auditoriaService.registrarAcaoPorUsuarioId(usuarioLogadoId, CategoriaAuditoria.USUARIO, "CRIAR",
                    "USUARIO", usuarioSalvo.getId_usuario().toString(),
                    "Usuário cadastrado pelo admin: " + usuarioSalvo.getEmail_usuario() + " (" + usuarioSalvo.getRole() + ")");
        }
        return UsuarioResponseDTO.fromEntity(usuarioSalvo);
    }

    @Transactional
    public LoginResponseDTO cadastrar(UsuarioCriacaoDTO dto) {
        if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
            throw new RegraNegocioException("EMAIL_DUPLICADO", "E-mail já cadastrado no sistema.");
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado para o ID: " + id));

        // Se o e-mail foi alterado, verificar duplicidade
        if (!usuario.getEmail_usuario().equalsIgnoreCase(dto.email_usuario())) {
            if (usuarioRepository.existsByEmail_usuario(dto.email_usuario())) {
                throw new RegraNegocioException("EMAIL_DUPLICADO", "E-mail já cadastrado por outro usuário.");
            }
            // Não permitir alterar o e-mail do Admin Geral
            if (masterAdminEmail.equalsIgnoreCase(usuario.getEmail_usuario())) {
                throw new RegraNegocioException("OPERACAO_NAO_PERMITIDA", "O e-mail do Administrador Geral não pode ser modificado.");
            }
            usuario.setEmail_usuario(dto.email_usuario());
        }

        usuario.setNome_usuario(dto.nome_usuario());
        usuario.setPhone_usuario(dto.phone_usuario());

        // Se for o Admin Geral, manter sempre como ADMIN
        if (masterAdminEmail.equalsIgnoreCase(usuario.getEmail_usuario())) {
            usuario.setRole(Role.ADMIN);
        } else if (dto.role() != null) {
            usuario.setRole(dto.role());
        }

        if (dto.nova_senha() != null && !dto.nova_senha().isBlank()) {
            usuario.setSenha_usuario(passwordEncoder.encode(dto.nova_senha()));
            usuario.incrementarTokenVersion();
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        if (auditoriaService != null) {
            auditoriaService.registrarAcaoPorUsuarioId(usuarioLogadoId, CategoriaAuditoria.USUARIO, "EDITAR",
                    "USUARIO", atualizado.getId_usuario().toString(),
                    "Usuário editado pelo admin: " + atualizado.getEmail_usuario() + " (" + atualizado.getRole() + ")");
        }
        return UsuarioResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public void excluirUsuario(Long id, Long usuarioLogadoId) {
        validarAcessoMasterAdmin(usuarioLogadoId);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado para o ID: " + id));

        if (masterAdminEmail.equalsIgnoreCase(usuario.getEmail_usuario())) {
            throw new RegraNegocioException("OPERACAO_NAO_PERMITIDA", "A conta do Administrador Geral não pode ser excluída.");
        }

        usuarioRepository.delete(usuario);
        if (auditoriaService != null) {
            auditoriaService.registrarAcaoPorUsuarioId(usuarioLogadoId, CategoriaAuditoria.USUARIO, "EXCLUIR",
                    "USUARIO", id.toString(),
                    "Usuário excluído pelo admin: " + usuario.getEmail_usuario() + " (" + usuario.getRole() + ")");
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(u -> UsuarioResponseDTO.fromEntity(u, u.isMasterAdmin(this.masterAdminEmail)))
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id, Long usuarioLogadoId) {
        if (usuarioLogadoId == null || (!usuarioLogadoId.equals(id) && !isMasterAdmin(usuarioLogadoId))) {
            throw new AccessDeniedException("Você não tem permissão para visualizar os dados de outro usuário.");
        }
        return buscarPorId(id);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado para o ID: " + id));
        return UsuarioResponseDTO.fromEntity(usuario, usuario.isMasterAdmin(this.masterAdminEmail));
    }

    @Transactional
    public Usuario obterOuCriarUsuarioBot(String nome, String telefone) {
        String telefoneSanitizado = telefone != null ? telefone.replaceAll("\\D", "") : "";
        if (telefoneSanitizado.length() < 10 || telefoneSanitizado.length() > 11) {
            throw new IllegalArgumentException("Número de telefone inválido para cadastro via bot (deve conter DDD + 8 ou 9 dígitos).");
        }
        
        return usuarioRepository.findByPhone_usuario(telefoneSanitizado)
                .orElseGet(() -> {
                    try {
                        String senhaBot = !botDefaultPassword.isBlank()
                                ? botDefaultPassword
                                : java.util.UUID.randomUUID().toString();

                        Usuario novoUsuario = Usuario.builder()
                                .nome_usuario(nome != null ? nome : "Usuário Bot")
                                .phone_usuario(telefoneSanitizado)
                                .email_usuario("bot_" + telefoneSanitizado + "@equadras.com")
                                .senha_usuario(passwordEncoder.encode(senhaBot))
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