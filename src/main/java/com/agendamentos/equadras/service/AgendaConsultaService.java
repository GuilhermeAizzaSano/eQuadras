package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.shared.pagination.PageResponse;
import com.agendamentos.equadras.shared.pagination.SortPolicy;
import com.agendamentos.equadras.specification.AgendamentoSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AgendaConsultaService {

    private static final Set<String> ORDENACOES_PERMITIDAS = Set.of("dataHoraInicio", "id");

    private static final SortPolicy SORT_POLICY_ATIVOS = SortPolicy.of(
            ORDENACOES_PERMITIDAS,
            Sort.by(Sort.Direction.ASC, "dataHoraInicio"),
            "id"
    );

    private static final SortPolicy SORT_POLICY_DESC = SortPolicy.of(
            ORDENACOES_PERMITIDAS,
            Sort.by(Sort.Direction.DESC, "dataHoraInicio"),
            "id"
    );

    private final AgendamentoRepository agendamentoRepository;
    private final QuadraRepository quadraRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public AgendaConsultaService(
            AgendamentoRepository agendamentoRepository,
            QuadraRepository quadraRepository,
            UsuarioRepository usuarioRepository,
            Clock clock
    ) {
        this.agendamentoRepository = agendamentoRepository;
        this.quadraRepository = quadraRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    private record IntervaloAgenda(LocalDateTime inicio, LocalDateTime fim) {}

    private IntervaloAgenda resolverIntervalo(LocalDate data, LocalDateTime inicio, LocalDateTime fim) {
        if (inicio != null && fim != null) {
            return new IntervaloAgenda(inicio, fim);
        }
        if (data != null) {
            return new IntervaloAgenda(data.atStartOfDay(), data.plusDays(1).atStartOfDay());
        }
        throw new IllegalArgumentException("Parâmetro 'data' ou intervalo ('inicio' e 'fim') é obrigatório.");
    }

    private void validarAcessoQuadra(Quadra quadra, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        boolean ehMasterAdmin = usuario.isMasterAdmin();
        boolean ehDonoQuadra = quadra.getAdmin() != null && quadra.getAdmin().getId_usuario().equals(usuarioId);

        if (!ehMasterAdmin && !ehDonoQuadra) {
            throw new AccessDeniedException("Você não tem permissão para visualizar o histórico desta quadra.");
        }
    }

    private Specification<Agendamento> escopoAgendaAdmin(Long usuarioId, IntervaloAgenda intervalo, Long quadraId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        Specification<Agendamento> spec = AgendamentoSpecifications.sobrepoeIntervalo(intervalo.inicio(), intervalo.fim());
        if (!usuario.isMasterAdmin()) {
            spec = spec.and(AgendamentoSpecifications.doAdmin(usuarioId));
        }
        if (quadraId != null) {
            Quadra quadra = quadraRepository.findById(quadraId)
                    .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));
            validarAcessoQuadra(quadra, usuarioId);
            spec = spec.and(AgendamentoSpecifications.daQuadra(quadraId));
        }
        return spec;
    }

    private List<AgendamentoResponseDTO> listarNaoCancelados(Specification<Agendamento> escopo) {
        Specification<Agendamento> spec = escopo.and((root, query, cb) -> cb.notEqual(root.get("status"), StatusAgendamento.CANCELADO));
        return agendamentoRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dataHoraInicio", "id"))
                .stream()
                .map(AgendamentoResponseDTO::fromEntitySemPix)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AgendamentoResponseDTO> listarAgendaDoDiaPaginado(
            Long usuarioId, LocalDate data, LocalDateTime inicio, LocalDateTime fim,
            Long quadraId, AbaAgendamento aba, Pageable pageable) {
        Specification<Agendamento> spec = escopoAgendaAdmin(usuarioId, resolverIntervalo(data, inicio, fim), quadraId);
        if (aba != null) {
            spec = spec.and(AgendamentoSpecifications.daAba(aba, LocalDateTime.now(clock)));
        }
        SortPolicy policy = (aba == AbaAgendamento.ATIVOS || aba == null) ? SORT_POLICY_ATIVOS : SORT_POLICY_DESC;
        Page<Agendamento> pagina = agendamentoRepository.findAll(spec, policy.apply(pageable));
        return PageResponse.of(pagina, AgendamentoResponseDTO::fromEntitySemPix);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarAgendaCompleta(
            Long usuarioId, LocalDate data, LocalDateTime inicio, LocalDateTime fim, Long quadraId) {
        IntervaloAgenda intervalo = resolverIntervalo(data, inicio, fim);
        if (intervalo.fim().isBefore(intervalo.inicio())
                || Duration.between(intervalo.inicio(), intervalo.fim()).toSeconds() > 86400) {
            throw new IllegalArgumentException("O intervalo não pode ser superior a 24 horas.");
        }
        return listarNaoCancelados(escopoAgendaAdmin(usuarioId, intervalo, quadraId));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarAgendaMensal(Long usuarioId, int ano, int mes, Long quadraId) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido: informe um valor entre 1 e 12.");
        }
        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("Ano inválido: informe um valor entre 2000 e 2100.");
        }
        YearMonth anoMes = YearMonth.of(ano, mes);
        IntervaloAgenda intervalo = new IntervaloAgenda(
                anoMes.atDay(1).atStartOfDay(),
                anoMes.plusMonths(1).atDay(1).atStartOfDay());
        return listarNaoCancelados(escopoAgendaAdmin(usuarioId, intervalo, quadraId));
    }

    @Transactional(readOnly = true)
    public Map<AbaAgendamento, Long> contarAgendaDoDiaPorAba(
            Long usuarioId, LocalDate data, LocalDateTime inicio, LocalDateTime fim, Long quadraId) {
        Specification<Agendamento> base = escopoAgendaAdmin(usuarioId, resolverIntervalo(data, inicio, fim), quadraId);
        LocalDateTime agora = LocalDateTime.now(clock);
        Map<AbaAgendamento, Long> contagens = new EnumMap<>(AbaAgendamento.class);
        for (AbaAgendamento aba : AbaAgendamento.values()) {
            contagens.put(aba, agendamentoRepository.count(base.and(AgendamentoSpecifications.daAba(aba, agora))));
        }
        return contagens;
    }
}
