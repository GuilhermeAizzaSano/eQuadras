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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgendamentoBotService {

    // 19, 9, 19h, 19 h, 19h30, 9:00, 19:00 e 19:00:00 (os segundos são ignorados)
    private static final Pattern PADRAO_HORA = Pattern.compile("^(\\d{1,2})(?:\\s*h\\s*(\\d{2})?|:(\\d{2})(?::\\d{2})?)?$");
    // 1900
    private static final Pattern PADRAO_HORA_COMPACTA = Pattern.compile("^(\\d{2})(\\d{2})$");

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

    public AgendamentoResponseDTO agendarViaBot(AgendamentoBotRequestDTO dto, boolean confirmarDireto) {
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

        return confirmarDireto
                ? agendamentoService.agendarConfirmado(criacaoDTO, usuario.getId_usuario())
                : agendamentoService.agendar(criacaoDTO, usuario.getId_usuario());
    }

    private LocalTime parseHora(String horaStr) {
        if (horaStr == null || horaStr.isBlank()) {
            throw new IllegalArgumentException("Hora não pode ser vazia.");
        }
        try {
            String normalizada = horaStr.trim().toLowerCase(Locale.ROOT);
            Matcher hora = PADRAO_HORA.matcher(normalizada);
            if (hora.matches()) {
                String minutos = hora.group(2) != null ? hora.group(2) : hora.group(3);
                return LocalTime.of(Integer.parseInt(hora.group(1)), minutos != null ? Integer.parseInt(minutos) : 0);
            }
            Matcher compacta = PADRAO_HORA_COMPACTA.matcher(normalizada);
            if (compacta.matches()) {
                return LocalTime.of(Integer.parseInt(compacta.group(1)), Integer.parseInt(compacta.group(2)));
            }
            throw new IllegalArgumentException("Formato de hora inválido: " + horaStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível entender a hora: " + horaStr);
        }
    }
}
