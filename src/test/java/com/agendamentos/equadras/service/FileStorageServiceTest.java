package com.agendamentos.equadras.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
    }

    @Test
    @DisplayName("Deve salvar arquivo JPEG com magic bytes validos")
    void deveSalvarArquivoJpegValido() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", jpegBytes);

        String path = fileStorageService.salvarArquivo(file);
        assertNotNull(path);
        assertTrue(path.startsWith("/uploads/quadras/"));
        assertTrue(path.endsWith(".jpg"));

        fileStorageService.excluirArquivo(path);
    }

    @Test
    @DisplayName("Deve salvar arquivo PNG com magic bytes validos")
    void deveSalvarArquivoPngValido() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", pngBytes);

        String path = fileStorageService.salvarArquivo(file);
        assertNotNull(path);
        assertTrue(path.startsWith("/uploads/quadras/"));
        assertTrue(path.endsWith(".png"));

        fileStorageService.excluirArquivo(path);
    }

    @Test
    @DisplayName("Deve salvar arquivo WebP com magic bytes validos")
    void deveSalvarArquivoWebpValido() {
        byte[] webpBytes = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "foto.webp", "image/webp", webpBytes);

        String path = fileStorageService.salvarArquivo(file);
        assertNotNull(path);
        assertTrue(path.startsWith("/uploads/quadras/"));
        assertTrue(path.endsWith(".webp"));

        fileStorageService.excluirArquivo(path);
    }

    @Test
    @DisplayName("Deve rejeitar arquivo falso/executavel disfarcado com extensao e mimetype de imagem")
    void deveRejeitarArquivoFalsoDisfarcado() {
        byte[] fakeExeBytes = "MZ\u0090\u0000\u0003\u0000\u0000\u0000\u0004\u0000\u0000\u0000\u00ff\u00ff".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "malware.png", "image/png", fakeExeBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> fileStorageService.salvarArquivo(file));
        assertTrue(ex.getMessage().contains("não corresponde a uma imagem válida"));
    }
}