package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.BloqueioHorario;
import com.agendamentos.equadras.model.entity.DisponibilidadeDia;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.model.enums.StatusHorario;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.BloqueioHorarioRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.util.DataFlexivelUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GradeHorariosService {

    private final QuadraRepository quadraRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final QuadraBuscaService quadraBuscaService;
    private final Clock clock;

    public GradeHorariosService(
            QuadraRepository quadraRepository,
            AgendamentoRepository agendamentoRepository,
            BloqueioHorarioRepository bloqueioHorarioRepository,
            UsuarioRepository usuarioRepository,
            QuadraBuscaService quadraBuscaService,
            Clock clock
    ) {
        this.quadraRepository = quadraRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.quadraBuscaService = quadraBuscaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponivelDTO> listarHorariosDisponiveis(Long quadraId, LocalDate data) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        DayOfWeek diaSemana = data.getDayOfWeek();
        DisponibilidadeDia disp = null;
        if (quadra.getDisponibilidades() != null) {
            disp = quadra.getDisponibilidades().stream()
                    .filter(d -> d.getDiaSemana() == diaSemana)
                    .findFirst()
                    .orElse(null);
        }

        if (disp == null) {
            return List.of();
        }

        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.atTime(LocalTime.MAX);

        List<Agendamento> agendamentosDoDia = agendamentoRepository.buscarPorQuadraEData(
                quadraId,
                StatusAgendamento.CANCELADO,
                inicioDoDia,
                fimDoDia
        );

        List<BloqueioHorario> bloqueios = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, data);
        return montarSlotsHorarios(quadra, data, agendamentosDoDia, bloqueios);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<HorarioDisponivelDTO>> listarHorariosDoDiaParaAdmin(LocalDate data, Long adminId) {
        Usuario admin = usuarioRepository.findById(adminId).orElse(null);
        List<Quadra> quadrasDoAdmin;
        if (admin != null && admin.isMasterAdmin()) {
            quadrasDoAdmin = quadraRepository.findAllWithAdminEFotos();
        } else {
            quadrasDoAdmin = quadraRepository.findByAdminId(adminId);
        }

        Map<Long, List<HorarioDisponivelDTO>> mapaResultado = new LinkedHashMap<>();
        List<Quadra> quadrasAtivas = quadrasDoAdmin.stream().filter(Quadra::isAtiva).toList();

        if (quadrasAtivas.isEmpty()) {
            for (Quadra quadra : quadrasDoAdmin) {
                mapaResultado.put(quadra.getId_quadra(), List.of());
            }
            return mapaResultado;
        }

        List<Long> quadraIdsAtivas = quadrasAtivas.stream().map(Quadra::getId_quadra).toList();
        Map<Long, List<Agendamento>> agendamentosPorQuadra = agendamentosPorQuadra(quadraIdsAtivas, data);
        Map<Long, List<BloqueioHorario>> bloqueiosPorQuadra = bloqueiosPorQuadra(quadraIdsAtivas, data);

        for (Quadra quadra : quadrasDoAdmin) {
            if (quadra.isAtiva()) {
                List<Agendamento> agendamentosQuadra = agendamentosPorQuadra.getOrDefault(quadra.getId_quadra(), List.of());
                List<BloqueioHorario> bloqueiosQuadra = bloqueiosPorQuadra.getOrDefault(quadra.getId_quadra(), List.of());
                List<HorarioDisponivelDTO> slots = montarSlotsHorarios(quadra, data, agendamentosQuadra, bloqueiosQuadra);
                mapaResultado.put(quadra.getId_quadra(), slots);
            } else {
                mapaResultado.put(quadra.getId_quadra(), List.of());
            }
        }

        return mapaResultado;
    }

    @Transactional(readOnly = true)
    public List<GradeHorariosResponseDTO> consultarGradeHorarios(
            LocalDate data, Long quadraId, String tipoEsporte, String nomeQuadra, boolean apenasDisponiveis) {

        List<Quadra> quadras = quadraBuscaService.buscarQuadrasAtivas(quadraId, tipoEsporte, nomeQuadra);
        if (quadras.isEmpty()) {
            return List.of();
        }

        List<Long> quadraIds = quadras.stream().map(Quadra::getId_quadra).toList();
        Map<Long, List<Agendamento>> agendamentosPorQuadra = agendamentosPorQuadra(quadraIds, data);
        Map<Long, List<BloqueioHorario>> bloqueiosPorQuadra = bloqueiosPorQuadra(quadraIds, data);

        List<GradeHorariosResponseDTO> resultado = new ArrayList<>();
        for (Quadra q : quadras) {
            List<HorarioDisponivelDTO> slots = montarSlotsHorarios(
                    q,
                    data,
                    agendamentosPorQuadra.getOrDefault(q.getId_quadra(), List.of()),
                    bloqueiosPorQuadra.getOrDefault(q.getId_quadra(), List.of()));
            if (apenasDisponiveis) {
                slots = slots.stream().filter(HorarioDisponivelDTO::disponivel).toList();
            }
            if (!slots.isEmpty() || !apenasDisponiveis) {
                resultado.add(new GradeHorariosResponseDTO(
                        q.getId_quadra(),
                        q.getNome(),
                        q.getTipoEsporte(),
                        q.getValorHora(),
                        data,
                        slots
                ));
            }
        }

        return resultado;
    }

    @Transactional(readOnly = true)
    public List<GradeHorariosResponseDTO> consultarGradeHorariosFlexivel(
            String dataFlexivel, Long quadraId, String tipoEsporte, String nomeQuadra, boolean apenasDisponiveis) {
        LocalDate dataResolvida = DataFlexivelUtil.resolverData(dataFlexivel);

        if (dataResolvida != null) {
            return consultarGradeHorarios(dataResolvida, quadraId, tipoEsporte, nomeQuadra, apenasDisponiveis);
        }

        LocalDate inicio = LocalDate.now(clock);
        List<GradeHorariosResponseDTO> resultadoFinal = new ArrayList<>();

        for (int i = 0; i < 14; i++) {
            LocalDate dataAlvo = inicio.plusDays(i);
            List<GradeHorariosResponseDTO> gradeDia =
                    consultarGradeHorarios(dataAlvo, quadraId, tipoEsporte, nomeQuadra, apenasDisponiveis);

            if (!gradeDia.isEmpty()) {
                resultadoFinal.addAll(gradeDia);
                return resultadoFinal;
            }
        }

        return resultadoFinal;
    }

    private Map<Long, List<Agendamento>> agendamentosPorQuadra(List<Long> quadraIds, LocalDate data) {
        return agendamentoRepository.buscarPorQuadrasEDataLote(
                        quadraIds,
                        StatusAgendamento.CANCELADO,
                        data.atStartOfDay(),
                        data.atTime(LocalTime.MAX))
                .stream()
                .collect(Collectors.groupingBy(a -> a.getQuadra().getId_quadra()));
    }

    private Map<Long, List<BloqueioHorario>> bloqueiosPorQuadra(List<Long> quadraIds, LocalDate data) {
        return bloqueioHorarioRepository.findByQuadraIdsAndData(quadraIds, data)
                .stream()
                .collect(Collectors.groupingBy(b -> b.getQuadra().getId_quadra()));
    }

    private List<HorarioDisponivelDTO> montarSlotsHorarios(
            Quadra quadra,
            LocalDate data,
            List<Agendamento> agendamentosDoDia,
            List<BloqueioHorario> bloqueios) {

        DayOfWeek diaSemana = data.getDayOfWeek();
        DisponibilidadeDia disp = null;
        if (quadra.getDisponibilidades() != null) {
            disp = quadra.getDisponibilidades().stream()
                    .filter(d -> d.getDiaSemana() == diaSemana)
                    .findFirst()
                    .orElse(null);
        }

        if (disp == null) {
            return List.of();
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        List<HorarioDisponivelDTO> slots = new ArrayList<>();

        boolean dataLimiteExcedida = quadra.getDataLimiteAgendamento() != null && data.isAfter(quadra.getDataLimiteAgendamento());
        BloqueioHorario bloqueioDiaInteiro = bloqueios.stream()
                .filter(b -> b.getHoraInicio() == null || b.getHoraFim() == null)
                .findFirst()
                .orElse(null);

        LocalTime slotInicio = disp.getHoraInicio();
        while (slotInicio.isBefore(disp.getHoraFim())) {
            LocalTime slotFim = slotInicio.plusHours(1);
            LocalDateTime slotDataHoraInicio = data.atTime(slotInicio);
            LocalDateTime slotDataHoraFim = data.atTime(slotFim);

            boolean disponivel = true;
            String motivo = "Disponível";
            StatusHorario status = StatusHorario.DISPONIVEL;

            if (!quadra.isAtiva()) {
                disponivel = false;
                status = StatusHorario.INDISPONIVEL;
                motivo = "Quadra inativa";
            } else if (dataLimiteExcedida) {
                disponivel = false;
                status = StatusHorario.BLOQUEADO;
                motivo = "Data limite de agendamento encerrada";
            } else if (bloqueioDiaInteiro != null) {
                disponivel = false;
                status = StatusHorario.BLOQUEADO;
                motivo = (bloqueioDiaInteiro.getMotivo() != null && !bloqueioDiaInteiro.getMotivo().isBlank())
                        ? "Bloqueado: " + bloqueioDiaInteiro.getMotivo()
                        : "Horário bloqueado pelo administrador";
            } else if (slotDataHoraInicio.isBefore(agora)) {
                disponivel = false;
                status = StatusHorario.INDISPONIVEL;
                motivo = "Horário indisponível (passado)";
            } else {
                final LocalTime sIni = slotInicio;
                final LocalTime sFim = slotFim;
                BloqueioHorario bloqueioParcial = bloqueios.stream()
                        .filter(b -> b.getHoraInicio() != null && b.getHoraFim() != null)
                        .filter(b -> b.getHoraInicio().isBefore(sFim) && b.getHoraFim().isAfter(sIni))
                        .findFirst()
                        .orElse(null);

                if (bloqueioParcial != null) {
                    disponivel = false;
                    status = StatusHorario.BLOQUEADO;
                    motivo = (bloqueioParcial.getMotivo() != null && !bloqueioParcial.getMotivo().isBlank())
                            ? "Bloqueado: " + bloqueioParcial.getMotivo()
                            : "Horário bloqueado pelo administrador";
                } else {
                    boolean ocupado = agendamentosDoDia.stream().anyMatch(a ->
                            a.getDataHoraInicio().isBefore(slotDataHoraFim) && a.getDataHoraFim().isAfter(slotDataHoraInicio)
                    );
                    if (ocupado) {
                        disponivel = false;
                        status = StatusHorario.AGENDADO;
                        motivo = "Horário ocupado / agendado";
                    }
                }
            }

            slots.add(new HorarioDisponivelDTO(slotInicio, slotFim, disponivel, status, motivo));
            slotInicio = slotFim;
        }

        return slots;
    }
}
