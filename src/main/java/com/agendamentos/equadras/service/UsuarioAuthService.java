package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AlterarSenhaDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.LoginResponseDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.exception.RecursoNaoEncontradoException;
import com.agendamentos.equadras.exception.RegraNegocioException;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAuthService {

    private static final String DUMMY_BCRYPT_HASH = "$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWXYZ012345";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;
    private final String masterAdminEmail;

    public UsuarioAuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditoriaService auditoriaService,
            @Value("${admin.master.email:gui@gmail.com}") String masterAdminEmail) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
        this.masterAdminEmail = masterAdminEmail != null ? masterAdminEmail.trim().toLowerCase() : "gui@gmail.com";
    }

    @Transactional
    public LoginResponseDTO login(UsuarioLoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail_usuario(dto.email_usuario())
                .orElse(null);

        if (usuario == null) {
            passwordEncoder.matches(dto.senha_usuario(), DUMMY_BCRYPT_HASH);
            if (auditoriaService != null) {
                auditoriaService.registrarLoginFalha(dto.email_usuario(), "E-mail não cadastrado.");
            }
            throw new RegraNegocioException("CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos.");
        }

        if (!passwordEncoder.matches(dto.senha_usuario(), usuario.getSenha_usuario())) {
            if (auditoriaService != null) {
                auditoriaService.registrarLoginFalha(dto.email_usuario(), "Senha incorreta.");
            }
            throw new RegraNegocioException("CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos.");
        }

        String token = jwtService.gerarToken(usuario);
        if (auditoriaService != null) {
            auditoriaService.registrarLoginSucesso(usuario);
        }
        boolean ehMaster = usuario.isMasterAdmin(this.masterAdminEmail);
        return new LoginResponseDTO(token, UsuarioResponseDTO.fromEntity(usuario, ehMaster));
    }

    @Transactional
    public void revogarSessao(Long usuarioId) {
        if (usuarioId == null) return;
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.incrementarTokenVersion();
            usuarioRepository.save(usuario);
        });
    }

    @Transactional
    public void alterarMinhaSenha(Long usuarioId, AlterarSenhaDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado para o ID: " + usuarioId));

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha_usuario())) {
            throw new RegraNegocioException("SENHA_INCORRETA", "A senha atual informada está incorreta.");
        }

        if (passwordEncoder.matches(dto.novaSenha(), usuario.getSenha_usuario())) {
            throw new RegraNegocioException("SENHA_REPETIDA", "A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenha_usuario(passwordEncoder.encode(dto.novaSenha()));
        usuario.incrementarTokenVersion();
        usuarioRepository.save(usuario);
        if (auditoriaService != null) {
            auditoriaService.registrarAlteracaoSenha(usuario);
        }
    }
}
