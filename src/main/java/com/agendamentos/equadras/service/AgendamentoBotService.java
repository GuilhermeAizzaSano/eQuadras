package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO;
import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.specification.QuadraSpecifications;
import com.agendamentos.equadras.util.DataFlexivelUtil;
import com.agendamentos.equadras.util.TextoUtil;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class AgendamentoBotService {

    private final QuadraRepository quadraRepository;
    private final UsuarioService usuarioService;
    private final AgendamentoService agendamentoService;
    private final Clock clock;

    public AgendamentoBotService(QuadraRepository quadraRepository,
                                 UsuarioService usuarioService,
                                 AgendamentoService agendamentoService,
                                 Clock clock) {
        this.quadraRepository = quadraRepository;
        this.usuarioService = usuarioService;
        this.agendamentoService = agendamentoService;
        this.clock = clock;
    }

    public AgendamentoResponseDTO agendarViaBot(AgendamentoBotRequestDTO dto) {
        Long quadraId = dto.quadraId();
        if (quadraId == null) {
            List<Quadra> quadras = buscarQuadrasAtivas(null, dto.tipoEsporte(), dto.nomeQuadra());
            if (quadras.isEmpty()) {
                throw new IllegalArgumentException("Nenhuma quadra encontrada para o esporte ou nome informado.");
            }
            quadraId = quadras.get(0).getId_quadra();
        }

        LocalDate data = DataFlexivelUtil.resolverData(dto.data());
        if (data == null) {
            data = LocalDate.now(clock);
        }

        LocalTime horaInicio = parseHora(dto.horaInicio());
        LocalTime horaFim;
        if (dto.horaFim() != null && !dto.horaFim().isBlank()) {
            horaFim = parseHora(dto.horaFim());
        } else {
            horaFim = horaInicio.plusHours(1);
        }

        if (!horaInicio.isBefore(horaFim)) {
            throw new IllegalArgumentException("Hora de início deve ser anterior à hora de término.");
        }

        Usuario usuario = usuarioService.obterOuCriarUsuarioBot(dto.nomeCliente(), dto.telefoneCliente());

        AgendamentoCriacaoDTO criacaoDTO = new AgendamentoCriacaoDTO(
                usuario.getId_usuario(),
                quadraId,
                data.atTime(horaInicio),
                data.atTime(horaFim)
        );

        return agendamentoService.agendar(criacaoDTO, usuario.getId_usuario());
    }

    private LocalTime parseHora(String horaStr) {
        if (horaStr == null || horaStr.isBlank()) {
            throw new IllegalArgumentException("Hora não pode ser vazia.");
        }
        try {
            if (horaStr.length() == 5 && horaStr.contains(":")) {
                return LocalTime.parse(horaStr);
            }
            if (horaStr.length() <= 2) {
                return LocalTime.of(Integer.parseInt(horaStr), 0);
            }
            String limpo = horaStr.replaceAll("[^0-9]", "");
            if (limpo.length() >= 4) {
                return LocalTime.of(Integer.parseInt(limpo.substring(0, 2)), Integer.parseInt(limpo.substring(2, 4)));
            }
            throw new IllegalArgumentException("Formato de hora inválido: " + horaStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível entender a hora: " + horaStr);
        }
    }

    private List<Quadra> buscarQuadrasAtivas(Long quadraId, String tipoEsporte, String nomeQuadra) {
        if (quadraId != null) {
            return quadraRepository.findById(quadraId)
                    .filter(Quadra::isAtiva)
                    .map(List::of)
                    .orElse(List.of());
        }

        Specification<Quadra> spec = QuadraSpecifications.ativa();

        if (tipoEsporte != null && !tipoEsporte.isBlank()) {
            TipoEsporte esporteEnum = parseTipoEsporte(tipoEsporte);
            if (esporteEnum != null) {
                spec = spec.and(QuadraSpecifications.comTipoEsporte(esporteEnum));
            } else {
                return List.of();
            }
        }

        if (nomeQuadra != null && !nomeQuadra.isBlank()) {
            spec = spec.and(QuadraSpecifications.comNome(nomeQuadra));
        }

        return quadraRepository.findAll(spec);
    }

    private TipoEsporte parseTipoEsporte(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = TextoUtil.normalizar(valor)
                .toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");

        for (TipoEsporte t : TipoEsporte.values()) {
            if (t.name().equals(normalizado)) {
                return t;
            }
        }

        if (normalizado.contains("SOCIETY") || normalizado.contains("CAMPO") || normalizado.contains("FUT")) {
            return TipoEsporte.FUTEBOL;
        }
        if (normalizado.contains("SALAO")) {
            return TipoEsporte.FUTSAL;
        }
        if (normalizado.contains("BEACH") || normalizado.contains("AREIA") || normalizado.contains("FUTEVOLEI") || normalizado.contains("BIT")) {
            return TipoEsporte.BEACH_TENNIS;
        }
        if (normalizado.contains("BASQUET")) {
            return TipoEsporte.BASQUETE;
        }
        if (normalizado.contains("TENIS")) {
            return TipoEsporte.TENIS;
        }
        if (normalizado.contains("VOLEI")) {
            return TipoEsporte.VOLEI;
        }

        return null;
    }
}
