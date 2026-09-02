package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.request.UsuarioLoginDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.agendamentos.equadras.security.JwtService jwtService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Mariana")
                .email_usuario("mariana@email.com")
                .senha_usuario("$2a$10$encodedPasswordHash")
                .phone_usuario("11988887777")
                .role(Role.CLIENT)
                .build();
    }

    @Test
    @DisplayName("Deve cadastrar usuário com senha criptografada e role CLIENT por padrão")
    void deveCadastrarUsuarioComSenhaCriptografada() {
        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO(
                "Mariana",
                "mariana@email.com",
                "senha123",
                "11988887777"
        );

        when(usuarioRepository.existsByEmail_usuario(dto.email_usuario())).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$encodedPasswordHash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(jwtService.gerarToken(any(Usuario.class))).thenReturn("token-fake");

        var resposta = usuarioService.cadastrar(dto);

        assertNotNull(resposta);
        assertEquals("token-fake", resposta.token());
        assertEquals("Mariana", resposta.usuario().nome_usuario());
        assertEquals("mariana@email.com", resposta.usuario().email_usuario());
        assertEquals(Role.CLIENT, resposta.usuario().role());

        verify(passwordEncoder, times(1)).encode("senha123");
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar cadastrar usuário com email já existente")
    void deveLancarErroQuandoEmailDuplicado() {
        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO(
                "Mariana",
                "mariana@email.com",
                "senha123",
                "11988887777"
        );

        when(usuarioRepository.existsByEmail_usuario(dto.email_usuario())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> usuarioService.cadastrar(dto));
        assertEquals("E-mail já cadastrado no sistema.", ex.getMessage());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve realizar login com sucesso com credenciais válidas")
    void deveRealizarLoginComSucesso() {
        UsuarioLoginDTO dto = new UsuarioLoginDTO("mariana@email.com", "senha123");

        when(usuarioRepository.findByEmail_usuario("mariana@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", usuario.getSenha_usuario())).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("token-fake");

        var resposta = usuarioService.login(dto);

        assertNotNull(resposta);
        assertEquals("token-fake", resposta.token());
        assertEquals(1L, resposta.usuario().id_usuario());
        assertEquals("mariana@email.com", resposta.usuario().email_usuario());
    }

    @Test
    @DisplayName("Deve falhar no login quando a senha estiver incorreta")
    void deveFalharLoginComSenhaIncorreta() {
        UsuarioLoginDTO dto = new UsuarioLoginDTO("mariana@email.com", "senhaErrada");

        when(usuarioRepository.findByEmail_usuario("mariana@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", usuario.getSenha_usuario())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> usuarioService.login(dto));
        assertEquals("E-mail ou senha incorretos.", ex.getMessage());
    }
}
