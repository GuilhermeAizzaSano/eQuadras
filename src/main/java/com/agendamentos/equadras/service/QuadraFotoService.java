package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class QuadraFotoService {

    private final QuadraRepository quadraRepository;
    private final UsuarioService usuarioService;
    private final FileStorageService fileStorageService;

    public QuadraFotoService(QuadraRepository quadraRepository,
                             UsuarioService usuarioService,
                             FileStorageService fileStorageService) {
        this.quadraRepository = quadraRepository;
        this.usuarioService = usuarioService;
        this.fileStorageService = fileStorageService;
    }

    private boolean podeGerenciarQuadra(Quadra quadra, Long adminId) {
        if (adminId == null) return false;
        Usuario admin = usuarioService.buscarPorIdEntidade(adminId).orElse(null);
        if (admin == null) return false;
        if (admin.isMasterAdmin()) return true;
        return quadra.getAdmin() != null && quadra.getAdmin().getId_usuario().equals(adminId);
    }

    @Transactional
    public QuadraResponseDTO uploadFotos(Long id, List<MultipartFile> arquivos, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new AccessDeniedException("Apenas o administrador dono da quadra ou o Master Admin pode fazer upload de fotos.");
        }

        if (arquivos == null || arquivos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo enviado.");
        }

        if (quadra.getFotos().size() + arquivos.size() > 5) {
            throw new IllegalArgumentException("Limite de 5 fotos por quadra atingido. Remova fotos existentes antes de enviar novas.");
        }

        for (MultipartFile file : arquivos) {
            String url = fileStorageService.salvarArquivo(file);
            quadra.getFotos().add(url);
        }

        Quadra quadraSalva = quadraRepository.save(quadra);
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    @Transactional
    public QuadraResponseDTO removerFoto(Long id, String fotoUrl, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!podeGerenciarQuadra(quadra, adminId)) {
            throw new AccessDeniedException("Apenas o administrador dono da quadra ou o Master Admin pode remover fotos.");
        }

        if (quadra.getFotos().remove(fotoUrl)) {
            fileStorageService.excluirArquivo(fotoUrl);
        }

        Quadra quadraSalva = quadraRepository.save(quadra);
        return QuadraResponseDTO.fromEntity(quadraSalva);
    }

    public void excluirFotosDaQuadra(Quadra quadra) {
        if (quadra != null && quadra.getFotos() != null) {
            for (String fotoUrl : quadra.getFotos()) {
                fileStorageService.excluirArquivo(fotoUrl);
            }
        }
    }
}
