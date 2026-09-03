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

    @Test
    @DisplayName("Deve cadastrar usuário quando chamado pelo Master Admin")
    void deveCadastrarPorAdminQuandoMasterAdmin() {
        Usuario masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Admin Geral")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();

        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO(
                "Novo Atleta",
                "atleta@email.com",
                "senha123",
                "11988887777",
                Role.CLIENT
        );

        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin));
        when(usuarioRepository.existsByEmail_usuario(dto.email_usuario())).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$encodedPasswordHash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId_usuario(10L);
            return u;
        });

        var response = usuarioService.cadastrarPorAdmin(dto, 99L);

        assertNotNull(response);
        assertEquals("Novo Atleta", response.nome_usuario());
        assertEquals(Role.CLIENT, response.role());
    }

    @Test
    @DisplayName("Deve barrar cadastro por admin quando não for o Master Admin")
    void deveBarrarCadastroPorAdminQuandoNaoMasterAdmin() {
        Usuario adminComum = Usuario.builder()
                .id_usuario(2L)
                .nome_usuario("Outro Admin")
                .email_usuario("outro@email.com")
                .role(Role.ADMIN)
                .build();

        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO(
                "Novo Atleta",
                "atleta@email.com",
                "senha123",
                "11988887777"
        );

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(adminComum));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> usuarioService.cadastrarPorAdmin(dto, 2L));
    }

    @Test
    @DisplayName("Deve impedir exclusão da conta do Master Admin")
    void deveImpedirExclusaoDoMasterAdmin() {
        Usuario masterAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Admin Geral")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();

        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.excluirUsuario(99L, 99L));
        assertTrue(ex.getMessage().contains("não pode ser excluída"));
    }

    @Test
    @DisplayName("Deve alterar a senha do próprio usuário com sucesso")
    void deveAlterarMinhaSenhaComSucesso() {
        com.agendamentos.equadras.dto.request.AlterarSenhaDTO dto =
                new com.agendamentos.equadras.dto.request.AlterarSenhaDTO("senhaAtual123", "NovaSenha@2026");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual123", usuario.getSenha_usuario())).thenReturn(true);
        when(passwordEncoder.encode("NovaSenha@2026")).thenReturn("$2a$10$newHashedPassword");

        usuarioService.alterarMinhaSenha(1L, dto);

        verify(usuarioRepository, times(1)).save(usuario);
        assertEquals("$2a$10$newHashedPassword", usuario.getSenha_usuario());
    }

    @Test
    @DisplayName("Deve falhar alteração de senha quando a senha atual informada estiver incorreta")
    void deveFalharQuandoSenhaAtualIncorreta() {
        com.agendamentos.equadras.dto.request.AlterarSenhaDTO dto =
                new com.agendamentos.equadras.dto.request.AlterarSenhaDTO("senhaErrada", "NovaSenha@2026");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", usuario.getSenha_usuario())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.alterarMinhaSenha(1L, dto));
        assertTrue(ex.getMessage().contains("A senha atual informada está incorreta"));
        verify(usuarioRepository, never()).save(any());
    }
}
