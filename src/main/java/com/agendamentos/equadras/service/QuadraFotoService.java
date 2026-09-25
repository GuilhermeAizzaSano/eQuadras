package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.QuadraFotosResponseDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class QuadraFotoService {

    private final QuadraRepository quadraRepository;
    private final UsuarioService usuarioService;
    private final FileStorageService fileStorageService;
    private final QuadraBuscaService quadraBuscaService;

    public QuadraFotoService(QuadraRepository quadraRepository,
                             UsuarioService usuarioService,
                             FileStorageService fileStorageService,
                             QuadraBuscaService quadraBuscaService) {
        this.quadraRepository = quadraRepository;
        this.usuarioService = usuarioService;
        this.fileStorageService = fileStorageService;
        this.quadraBuscaService = quadraBuscaService;
    }

    /**
     * Contrato legado (bot/integrações): por id → objeto (inclusive quadra inativa); filtro com 1 resultado → objeto;
     * vários → lista; nenhum → {"fotos": []}; sem filtro → lista de todas as ativas.
     */
    @Transactional(readOnly = true)
    public Object consultarFotos(Long id, String nome, String tipoEsporte, String cidade, String bairro) {
        if (id != null) {
            Quadra quadra = quadraRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));
            return QuadraFotosResponseDTO.fromEntity(quadra);
        }

        List<QuadraFotosResponseDTO> quadras = quadraBuscaService
                .filtrarQuadrasEntidades(null, null, null, null, tipoEsporte, nome, cidade, bairro, null)
                .stream()
                .map(QuadraFotosResponseDTO::fromEntity)
                .toList();

        boolean temFiltro = temTexto(nome) || temTexto(tipoEsporte) || temTexto(cidade) || temTexto(bairro);
        if (!temFiltro) {
            return quadras;
        }
        if (quadras.size() == 1) {
            return quadras.get(0);
        }
        return quadras.isEmpty() ? Map.of("fotos", List.of()) : quadras;
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Transactional
    public QuadraResponseDTO uploadFotos(Long id, List<MultipartFile> arquivos, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + id));

        if (!usuarioService.podeGerenciarQuadra(quadra, adminId)) {
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

        if (!usuarioService.podeGerenciarQuadra(quadra, adminId)) {
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
