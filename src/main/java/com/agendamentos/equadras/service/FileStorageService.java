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

        String novoNome = UUID.randomUUID() + "." + extension;
        Path destino = Paths.get(UPLOAD_DIR).resolve(novoNome);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/quadras/" + novoNome;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar a imagem no servidor.", e);
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
