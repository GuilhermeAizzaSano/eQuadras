package com.agendamentos.equadras.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class FileStorageService {

    private static final String UPLOAD_DIR = "uploads/quadras";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileStorageService() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível inicializar o diretório de uploads: " + UPLOAD_DIR, e);
        }
    }

    public String salvarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo enviado está vazio.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("O arquivo ultrapassa o tamanho máximo permitido de 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato de arquivo inválido. Apenas JPG, PNG e WebP são permitidos.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Extensão de arquivo não permitida.");
        }

        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.readNBytes(header, 0, 12);
            if (read < 12) {
                throw new IllegalArgumentException("Arquivo muito pequeno ou corrompido.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o cabeçalho do arquivo.", e);
        }
        validarMagicBytes(header);

        String novoNome = UUID.randomUUID() + "." + extension;
        Path destino = Paths.get(UPLOAD_DIR).resolve(novoNome);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/quadras/" + novoNome;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar a imagem no servidor.", e);
        }
    }

    public void validarMagicBytes(byte[] header) {
        if (header == null || header.length < 12) {
            throw new IllegalArgumentException("Arquivo inválido ou corrompido.");
        }

        // JPEG: FF D8 FF
        boolean isJpeg = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;

        // WEBP: RIFF....WEBP
        boolean isWebp = header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';

        if (!isJpeg && !isPng && !isWebp) {
            throw new IllegalArgumentException("Conteúdo do arquivo não corresponde a uma imagem válida (JPEG, PNG ou WebP).");
        }
    }

    public void excluirArquivo(String urlRelativa) {
        if (urlRelativa == null || !urlRelativa.startsWith("/uploads/quadras/")) {
            return;
        }
        String nomeArquivo = urlRelativa.replace("/uploads/quadras/", "");
        Path caminho = Paths.get(UPLOAD_DIR).resolve(nomeArquivo);
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException e) {
            System.err.println("Aviso: não foi possível remover arquivo " + caminho);
        }
    }
}
