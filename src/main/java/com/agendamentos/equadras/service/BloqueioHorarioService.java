package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.BloqueioHorarioCriacaoDTO;
import com.agendamentos.equadras.dto.response.BloqueioHorarioResponseDTO;
import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.repository.BloqueioHorarioRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BloqueioHorarioService {

    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final QuadraRepository quadraRepository;

    public BloqueioHorarioService(BloqueioHorarioRepository bloqueioHorarioRepository, QuadraRepository quadraRepository) {
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
        this.quadraRepository = quadraRepository;
    }

    @Transactional
    public BloqueioHorarioResponseDTO criarBloqueio(Long quadraId, BloqueioHorarioCriacaoDTO dto, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        if (quadra.getAdmin() != null && !quadra.getAdmin().getId_usuario().equals(adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra pode criar bloqueios.");
        }

        if (dto.data().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data do bloqueio não pode ser no passado.");
        }

        if (dto.horaInicio() != null && dto.horaFim() != null) {
            if (!dto.horaInicio().isBefore(dto.horaFim())) {
                throw new IllegalArgumentException("A hora de início deve ser anterior à hora de término.");
            }
        } else if (dto.horaInicio() != null || dto.horaFim() != null) {
            throw new IllegalArgumentException("Para bloqueios com horário, ambos os horários (início e fim) devem ser fornecidos.");
        }

        BloqueioHorario bloqueio = new BloqueioHorario(quadra, dto.data(), dto.horaInicio(), dto.horaFim(), dto.motivo());
        BloqueioHorario salvo = bloqueioHorarioRepository.save(bloqueio);
        return BloqueioHorarioResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<BloqueioHorarioResponseDTO> listarBloqueios(Long quadraId) {
        return bloqueioHorarioRepository.findByQuadraId(quadraId)
                .stream()
                .map(BloqueioHorarioResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void removerBloqueio(Long quadraId, Long bloqueioId, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        if (quadra.getAdmin() != null && !quadra.getAdmin().getId_usuario().equals(adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra pode remover bloqueios.");
        }

        BloqueioHorario bloqueio = bloqueioHorarioRepository.findById(bloqueioId)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio não encontrado para o ID: " + bloqueioId));

        if (!bloqueio.getQuadra().getId_quadra().equals(quadraId)) {
            throw new IllegalArgumentException("O bloqueio informado não pertence a esta quadra.");
        }

        bloqueioHorarioRepository.delete(bloqueio);
    }

    @Transactional
    public int desbloquearHorarios(Long quadraId, com.agendamentos.equadras.dto.request.DesbloqueioHorarioDTO dto, Long adminId) {
        Quadra quadra = quadraRepository.findByIdWithAdmin(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        if (quadra.getAdmin() != null && !quadra.getAdmin().getId_usuario().equals(adminId)) {
            throw new IllegalArgumentException("Apenas o administrador dono da quadra pode remover bloqueios.");
        }

        if (dto.bloqueioId() != null) {
            removerBloqueio(quadraId, dto.bloqueioId(), adminId);
            return 1;
        }

        if (dto.data() == null) {
            throw new IllegalArgumentException("Informe o ID do bloqueio ou a data a ser desbloqueada.");
        }

        List<BloqueioHorario> bloqueios = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, dto.data());
        if (bloqueios.isEmpty()) {
            return 0;
        }

        List<BloqueioHorario> paraRemover;
        if (dto.horaInicio() != null && dto.horaFim() != null) {
            paraRemover = bloqueios.stream()
                    .filter(b -> (b.getHoraInicio() != null && b.getHoraFim() != null &&
                            b.getHoraInicio().equals(dto.horaInicio()) && b.getHoraFim().equals(dto.horaFim()))
                            || (b.getHoraInicio() == null && b.getHoraFim() == null))
                    .toList();
        } else {
            paraRemover = bloqueios;
        }

        bloqueioHorarioRepository.deleteAll(paraRemover);
        return paraRemover.size();
    }
}
