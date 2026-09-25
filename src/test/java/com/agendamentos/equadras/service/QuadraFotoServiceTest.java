package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuadraFotoServiceTest {

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private QuadraBuscaService quadraBuscaService;

    @InjectMocks
    private QuadraFotoService quadraFotoService;

    private Usuario admin;
    private Usuario masterAdmin;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        admin = Usuario.builder()
                .id_usuario(1L)
                .nome_usuario("Admin")
                .email_usuario("admin@equadras.com")
                .role(Role.ADMIN)
                .build();

        masterAdmin = Usuario.builder()
                .id_usuario(2L)
                .nome_usuario("Master")
                .email_usuario("gui@gmail.com")
                .role(Role.ADMIN)
                .build();

        quadra = Quadra.builder()
                .id_quadra(10L)
                .nome("Quadra Society")
                .admin(admin)
                .ativa(true)
                .fotos(new ArrayList<>(List.of("http://foto1.jpg")))
                .build();
    }

    @Test
    @DisplayName("Deve fazer upload de fotos com sucesso respeitando o limite de 5 fotos")
    void deveFazerUploadDeFotosComSucesso() {
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(usuarioService.podeGerenciarQuadra(any(), eq(1L))).thenReturn(true);
        when(fileStorageService.salvarArquivo(any())).thenReturn("http://foto2.jpg");
        when(quadraRepository.save(any(Quadra.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("fotos", "foto2.jpg", "image/jpeg", "conteudo".getBytes());
        QuadraResponseDTO resposta = quadraFotoService.uploadFotos(10L, List.of(file), 1L);

        assertNotNull(resposta);
        assertEquals(2, quadra.getFotos().size());
        assertTrue(quadra.getFotos().contains("http://foto2.jpg"));
        verify(fileStorageService, times(1)).salvarArquivo(file);
        verify(quadraRepository, times(1)).save(quadra);
    }

    @Test
    @DisplayName("Deve rejeitar upload de fotos quando exceder o limite de 5 fotos")
    void deveRejeitarUploadAcimaDoLimite() {
        quadra.setFotos(new ArrayList<>(List.of("f1", "f2", "f3", "f4", "f5")));
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(usuarioService.podeGerenciarQuadra(any(), eq(1L))).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("fotos", "nova.jpg", "image/jpeg", "bytes".getBytes());
        List<MultipartFile> arquivos = List.of(file);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                quadraFotoService.uploadFotos(10L, arquivos, 1L));

        assertTrue(ex.getMessage().contains("Limite de 5 fotos"));
        verify(fileStorageService, never()).salvarArquivo(any());
    }

    @Test
    @DisplayName("Deve barrar upload de fotos por administrador que não seja dono da quadra")
    void deveBarrarUploadPorNaoProprietario() {
        Usuario outroAdmin = Usuario.builder()
                .id_usuario(99L)
                .nome_usuario("Outro")
                .role(Role.ADMIN)
                .build();

        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(usuarioService.podeGerenciarQuadra(any(), eq(99L))).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", "bytes".getBytes());
        List<MultipartFile> arquivos = List.of(file);

        assertThrows(AccessDeniedException.class, () ->
                quadraFotoService.uploadFotos(10L, arquivos, 99L));
        verify(fileStorageService, never()).salvarArquivo(any());
    }

    @Test
    @DisplayName("Deve remover foto da quadra e excluir arquivo físico")
    void deveRemoverFotoComSucesso() {
        when(quadraRepository.findByIdWithAdmin(10L)).thenReturn(Optional.of(quadra));
        when(usuarioService.podeGerenciarQuadra(any(), eq(1L))).thenReturn(true);
        when(quadraRepository.save(any(Quadra.class))).thenAnswer(inv -> inv.getArgument(0));

        QuadraResponseDTO resposta = quadraFotoService.removerFoto(10L, "http://foto1.jpg", 1L);

        assertNotNull(resposta);
        assertFalse(quadra.getFotos().contains("http://foto1.jpg"));
        verify(fileStorageService, times(1)).excluirArquivo("http://foto1.jpg");
        verify(quadraRepository, times(1)).save(quadra);
    }

    @Test
    @DisplayName("Deve excluir arquivos físicos de todas as fotos da quadra")
    void deveExcluirFotosFisicasDaQuadra() {
        quadra.setFotos(List.of("http://foto1.jpg", "http://foto2.jpg"));

        quadraFotoService.excluirFotosDaQuadra(quadra);

        verify(fileStorageService, times(1)).excluirArquivo("http://foto1.jpg");
        verify(fileStorageService, times(1)).excluirArquivo("http://foto2.jpg");
    }
}
