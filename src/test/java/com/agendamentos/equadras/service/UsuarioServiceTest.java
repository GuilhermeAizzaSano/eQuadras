package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.exception.RecursoNaoEncontradoException;
import com.agendamentos.equadras.exception.RegraNegocioException;
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

    @Mock
    private AuditoriaService auditoriaService;

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

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> usuarioService.cadastrar(dto));
        assertEquals("E-mail já cadastrado no sistema.", ex.getMessage());

        verify(usuarioRepository, never()).save(any(Usuario.class));
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

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> usuarioService.excluirUsuario(99L, 99L));
        assertTrue(ex.getMessage().contains("não pode ser excluída"));
    }

    @Test
    @DisplayName("Deve buscar entidade usuário por ID com sucesso")
    void deveBuscarPorIdEntidadeComSucesso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = usuarioService.buscarPorIdEntidade(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Mariana", resultado.get().getNome_usuario());
        verify(usuarioRepository, times(1)).findById(1L);
    }
}

