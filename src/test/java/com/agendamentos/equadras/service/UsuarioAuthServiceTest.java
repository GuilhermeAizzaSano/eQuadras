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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioAuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditoriaService auditoriaService;

    private UsuarioAuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        authService = new UsuarioAuthService(
                usuarioRepository,
                passwordEncoder,
                jwtService,
                auditoriaService,
                "gui@gmail.com"
        );

        usuario = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Mariana")
                .email_usuario("mariana@email.com")
                .senha_usuario("$2a$10$encodedPasswordHash")
                .phone_usuario("11988887777")
                .role(com.agendamentos.equadras.model.enums.Role.CLIENT)
                .build();
    }

    @Test
    @DisplayName("Deve realizar login com credenciais válidas e retornar token com perfil")
    void deveRealizarLoginComSucesso() {
        UsuarioLoginDTO dto = new UsuarioLoginDTO("mariana@email.com", "senhaCorreta");
        when(usuarioRepository.findByEmail_usuario("mariana@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", usuario.getSenha_usuario())).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("jwt.token.mock");

        LoginResponseDTO resposta = authService.login(dto);

        assertNotNull(resposta);
        assertEquals("jwt.token.mock", resposta.token());
        assertEquals("mariana@email.com", resposta.usuario().email_usuario());
        verify(auditoriaService, times(1)).registrarLoginSucesso(usuario);
    }

    @Test
    @DisplayName("Deve executar verificação dummy quando usuário não existir para evitar timing attack")
    void deveExecutarDummyPasswordQuandoUsuarioNaoExiste() {
        UsuarioLoginDTO dto = new UsuarioLoginDTO("inexistente@email.com", "qualquerSenha");
        when(usuarioRepository.findByEmail_usuario("inexistente@email.com")).thenReturn(Optional.empty());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> authService.login(dto));

        assertEquals("E-mail ou senha incorretos.", ex.getMessage());
        verify(passwordEncoder, times(1)).matches(eq("qualquerSenha"), anyString());
        verify(auditoriaService, times(1)).registrarLoginFalha("inexistente@email.com", "E-mail não cadastrado.");
    }

    @Test
    @DisplayName("Deve falhar login quando a senha estiver errada")
    void deveFalharLoginQuandoSenhaIncorreta() {
        UsuarioLoginDTO dto = new UsuarioLoginDTO("mariana@email.com", "senhaErrada");
        when(usuarioRepository.findByEmail_usuario("mariana@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", usuario.getSenha_usuario())).thenReturn(false);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> authService.login(dto));

        assertEquals("E-mail ou senha incorretos.", ex.getMessage());
        verify(auditoriaService, times(1)).registrarLoginFalha("mariana@email.com", "Senha incorreta.");
    }

    @Test
    @DisplayName("Deve revogar sessão incrementando tokenVersion")
    void deveRevogarSessao() {
        int versaoInicial = usuario.getTokenVersion();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        authService.revogarSessao(1L);

        assertEquals(versaoInicial + 1, usuario.getTokenVersion());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    @DisplayName("Deve alterar senha própria com sucesso")
    void deveAlterarMinhaSenhaComSucesso() {
        AlterarSenhaDTO dto = new AlterarSenhaDTO("senhaAtual123", "NovaSenha@2026");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual123", usuario.getSenha_usuario())).thenReturn(true);
        when(passwordEncoder.matches("NovaSenha@2026", usuario.getSenha_usuario())).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha@2026")).thenReturn("$2a$10$newHashedPassword");

        authService.alterarMinhaSenha(1L, dto);

        verify(usuarioRepository, times(1)).save(usuario);
        assertEquals("$2a$10$newHashedPassword", usuario.getSenha_usuario());
        verify(auditoriaService, times(1)).registrarAlteracaoSenha(usuario);
    }

    @Test
    @DisplayName("Deve falhar alteração de senha quando senha atual estiver incorreta")
    void deveFalharQuandoSenhaAtualIncorreta() {
        AlterarSenhaDTO dto = new AlterarSenhaDTO("senhaErrada", "NovaSenha@2026");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", usuario.getSenha_usuario())).thenReturn(false);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> authService.alterarMinhaSenha(1L, dto));

        assertTrue(ex.getMessage().contains("A senha atual informada está incorreta"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar alteração de senha quando nova senha for idêntica à atual")
    void deveFalharQuandoNovaSenhaIdentica() {
        AlterarSenhaDTO dto = new AlterarSenhaDTO("senhaAtual123", "senhaAtual123");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual123", usuario.getSenha_usuario())).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> authService.alterarMinhaSenha(1L, dto));

        assertTrue(ex.getMessage().contains("A nova senha deve ser diferente da senha atual"));
        verify(usuarioRepository, never()).save(any());
    }
}
