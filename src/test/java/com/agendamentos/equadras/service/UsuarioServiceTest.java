package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.UsuarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.UsuarioResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
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
    private AuditoriaService auditoriaService;

    @Mock
    private com.agendamentos.equadras.security.ApiKeyCache apiKeyCache;

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

    @Test
    @DisplayName("Deve permitir ao dono gerenciar a própria quadra")
    void devePermitirDonoGerenciarQuadra() {
        Usuario dono = Usuario.builder().id_usuario(5L).email_usuario("dono@x.com").role(Role.ADMIN).build();
        Quadra quadra = Quadra.builder().id_quadra(1L).admin(dono).build();
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(dono));

        assertTrue(usuarioService.podeGerenciarQuadra(quadra, 5L));
    }

    @Test
    @DisplayName("Deve permitir ao Master Admin gerenciar quadra de outro administrador")
    void devePermitirMasterAdminGerenciarQuadra() {
        Usuario dono = Usuario.builder().id_usuario(5L).role(Role.ADMIN).build();
        Usuario master = Usuario.builder().id_usuario(99L).email_usuario("gui@gmail.com").role(Role.ADMIN).build();
        Quadra quadra = Quadra.builder().id_quadra(1L).admin(dono).build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(master));

        assertTrue(usuarioService.podeGerenciarQuadra(quadra, 99L));
    }

    @Test
    @DisplayName("Deve negar gerenciar quadra a outro admin, a id nulo e a quadra sem dono")
    void deveNegarGerenciarQuadraSemPermissao() {
        Usuario dono = Usuario.builder().id_usuario(5L).role(Role.ADMIN).build();
        Usuario outro = Usuario.builder().id_usuario(6L).email_usuario("outro@x.com").role(Role.ADMIN).build();
        Quadra quadra = Quadra.builder().id_quadra(1L).admin(dono).build();
        Quadra semDono = Quadra.builder().id_quadra(2L).build();
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(outro));

        assertFalse(usuarioService.podeGerenciarQuadra(quadra, 6L));
        assertFalse(usuarioService.podeGerenciarQuadra(semDono, 6L));
        assertFalse(usuarioService.podeGerenciarQuadra(quadra, null));
    }

    private Usuario masterAdmin() {
        return Usuario.builder().id_usuario(99L).nome_usuario("Admin Geral").email_usuario("gui@gmail.com").role(Role.ADMIN).build();
    }

    @Test
    @DisplayName("V16: cadastro grava o telefone só com dígitos")
    void cadastroNormalizaTelefone() {
        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO("Novo", "novo@email.com", "senha123", "(11) 99999-0016", Role.CLIENT);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));
        when(usuarioRepository.existeTelefoneEmOutroUsuario("11999990016", null)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId_usuario(10L);
            return u;
        });

        usuarioService.cadastrarPorAdmin(dto, 99L);

        verify(usuarioRepository).save(argThat(u -> "11999990016".equals(u.getPhone_usuario())));
    }

    @Test
    @DisplayName("V16: cadastro com telefone já usado retorna TELEFONE_EM_USO")
    void cadastroComTelefoneRepetido() {
        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO("Novo", "novo@email.com", "senha123", "(11) 99999-0016", Role.CLIENT);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));
        when(usuarioRepository.existeTelefoneEmOutroUsuario("11999990016", null)).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> usuarioService.cadastrarPorAdmin(dto, 99L));
        assertEquals("TELEFONE_EM_USO", ex.getCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("V16: edição para telefone de outro usuário retorna TELEFONE_EM_USO")
    void edicaoComTelefoneDeOutro() {
        var dto = new com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO("Mariana", "mariana@email.com", "11 97777-6666", Role.CLIENT, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existeTelefoneEmOutroUsuario("11977776666", 1L)).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> usuarioService.editarUsuario(1L, dto, 99L));
        assertEquals("TELEFONE_EM_USO", ex.getCode());
    }

    @Test
    @DisplayName("R5: cadastro com telefone sem DDD retorna TELEFONE_INVALIDO")
    void cadastroComTelefoneSemDdd() {
        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO("Novo", "novo@email.com", "senha123", "9999-0016", Role.CLIENT);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> usuarioService.cadastrarPorAdmin(dto, 99L));
        assertEquals("TELEFONE_INVALIDO", ex.getCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("R5: edição com telefone sem DDD retorna TELEFONE_INVALIDO")
    void edicaoComTelefoneSemDdd() {
        var dto = new com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO("Mariana", "mariana@email.com", "99999-0016", Role.CLIENT, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> usuarioService.editarUsuario(1L, dto, 99L));
        assertEquals("TELEFONE_INVALIDO", ex.getCode());
    }

    @Test
    @DisplayName("E-mail: cadastro grava o e-mail em minúsculas e sem espaços")
    void cadastroNormalizaEmail() {
        UsuarioCriacaoDTO dto = new UsuarioCriacaoDTO("Novo", "  Novo@Email.com ", "senha123", "11999990016", Role.CLIENT);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));
        when(usuarioRepository.existsByEmail_usuario("novo@email.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId_usuario(10L);
            return u;
        });

        usuarioService.cadastrarPorAdmin(dto, 99L);

        verify(usuarioRepository).save(argThat(u -> "novo@email.com".equals(u.getEmail_usuario())));
    }

    @Test
    @DisplayName("E-mail: trocar só a caixa do próprio e-mail não acusa duplicidade e grava minúsculo")
    void edicaoMudandoSoACaixa() {
        usuario.setEmail_usuario("Mariana@Email.com");
        var dto = new com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO("Mariana", "MARIANA@email.com", "11988887777", Role.CLIENT, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        usuarioService.editarUsuario(1L, dto, 99L);

        verify(usuarioRepository, never()).existsByEmail_usuario(any());
        assertEquals("mariana@email.com", usuario.getEmail_usuario());
    }

    @Test
    @DisplayName("E-mail: edição para o e-mail de outro usuário em outra caixa retorna EMAIL_DUPLICADO")
    void edicaoParaEmailDeOutroEmOutraCaixa() {
        var dto = new com.agendamentos.equadras.dto.request.UsuarioEdicaoDTO("Mariana", "OUTRO@Email.com", "11988887777", Role.CLIENT, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(masterAdmin()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmail_usuario("outro@email.com")).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> usuarioService.editarUsuario(1L, dto, 99L));
        assertEquals("EMAIL_DUPLICADO", ex.getCode());
    }

    @Test
    @DisplayName("V16: bot aceita telefone com +55 e encontra o cliente existente")
    void botNormalizaDdi() {
        when(usuarioRepository.findByPhone_usuario("11988887777")).thenReturn(Optional.of(usuario));

        assertSame(usuario, usuarioService.obterOuCriarUsuarioBot("Mariana", "+55 11 98888-7777"));
    }
}
