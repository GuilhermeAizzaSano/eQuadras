package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO;
import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.util.DataFlexivelUtil;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AgendamentoBotService {

    private final QuadraBuscaService quadraBuscaService;
    private final UsuarioService usuarioService;
    private final AgendamentoService agendamentoService;
    private final Clock clock;

    public AgendamentoBotService(QuadraBuscaService quadraBuscaService,
                                 UsuarioService usuarioService,
                                 AgendamentoService agendamentoService,
                                 Clock clock) {
        this.quadraBuscaService = quadraBuscaService;
        this.usuarioService = usuarioService;
        this.agendamentoService = agendamentoService;
        this.clock = clock;
    }

    public AgendamentoResponseDTO agendarViaBot(AgendamentoBotRequestDTO dto) {
        Long quadraId = dto.quadraId();
        if (quadraId == null) {
            List<Quadra> quadras = quadraBuscaService.buscarQuadrasAtivas(null, dto.tipoEsporte(), dto.nomeQuadra());
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
}
