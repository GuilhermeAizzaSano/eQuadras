package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.DashboardMetricasDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.event.AgendamentoCanceladoEvent;
import com.agendamentos.equadras.event.AgendamentoPagamentoConfirmadoEvent;
import com.agendamentos.equadras.event.AgendamentosExpiradosCanceladosEvent;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.exception.RecursoNaoEncontradoException;
import com.agendamentos.equadras.exception.RegraNegocioException;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.shared.pagination.PageResponse;
import com.agendamentos.equadras.shared.pagination.SortPolicy;
import com.agendamentos.equadras.specification.AgendamentoSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class AgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    private static final LocalTime HORARIO_ABERTURA = LocalTime.of(6, 0);
    private static final LocalTime HORARIO_FECHAMENTO = LocalTime.of(23, 0);

    private static final Set<String> ORDENACOES_PERMITIDAS_AGENDAMENTO = Set.of("dataHoraInicio", "id");

    private static final SortPolicy SORT_POLICY_ATIVOS = SortPolicy.of(
            ORDENACOES_PERMITIDAS_AGENDAMENTO,
            Sort.by(Sort.Direction.ASC, "dataHoraInicio"),
            "id"
    );

    private static final SortPolicy SORT_POLICY_DESC = SortPolicy.of(
            ORDENACOES_PERMITIDAS_AGENDAMENTO,
            Sort.by(Sort.Direction.DESC, "dataHoraInicio"),
            "id"
    );

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final QuadraRepository quadraRepository;
    private final PagamentoService pagamentoService;
    private final AgendamentoLockService agendamentoLockService;
    private final com.agendamentos.equadras.repository.BloqueioHorarioRepository bloqueioHorarioRepository;
    private final QuadraService quadraService;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher eventPublisher;
    private final java.time.Clock clock;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              UsuarioRepository usuarioRepository,
                              QuadraRepository quadraRepository,
                              PagamentoService pagamentoService,
                              AgendamentoLockService agendamentoLockService,
                              com.agendamentos.equadras.repository.BloqueioHorarioRepository bloqueioHorarioRepository,
                              @org.springframework.context.annotation.Lazy QuadraService quadraService,
                              UsuarioService usuarioService,
                              ApplicationEventPublisher eventPublisher,
                              java.time.Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.quadraRepository = quadraRepository;
        this.pagamentoService = pagamentoService;
        this.agendamentoLockService = agendamentoLockService;
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
        this.quadraService = quadraService;
        this.usuarioService = usuarioService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public AgendamentoResponseDTO agendar(AgendamentoCriacaoDTO dto, Long usuarioIdAutenticado) {
        if (!dto.dataHoraFim().isAfter(dto.dataHoraInicio())) {
            throw new RegraNegocioException("INTERVALO_INVALIDO", "A data/hora de término deve ser posterior à data/hora de início.");
        }

        if (dto.dataHoraInicio().isBefore(LocalDateTime.now(clock))) {
            throw new RegraNegocioException("HORARIO_PASSADO", "Não é possível realizar agendamentos em horários passados.");
        }

        // 1. Cria o agendamento em transação com lock pessimista na quadra e commita imediatamente
        Agendamento agendamentoSalvo = agendamentoLockService.criarAgendamentoPendenteComLock(dto, usuarioIdAutenticado);

        PagamentoService.PixDados pixDados;
        try {
            // 2. Chama API externa FORA da transação e do lock do banco
            pixDados = pagamentoService.gerarPix(agendamentoSalvo);
        } catch (Exception e) {
            log.error("Falha ao gerar cobrança Pix para agendamento {}. Executando compensação imediata.", agendamentoSalvo.getId_agendamento(), e);
            try {
                agendamentoSalvo.setStatus(StatusAgendamento.CANCELADO);
                agendamentoSalvo.setCanceladoEm(LocalDateTime.now(clock));
                agendamentoRepository.save(agendamentoSalvo);
            } catch (Exception exCompensacao) {
                log.error("Erro crítico ao tentar cancelar agendamento órfão {}", agendamentoSalvo.getId_agendamento(), exCompensacao);
            }
            throw new RegraNegocioException("FALHA_GATEWAY_PAGAMENTO", "Não foi possível gerar a cobrança Pix no gateway de pagamento. O slot foi liberado.", e);
        }

        // 3. Atualiza os dados Pix em nova transação leve
        Agendamento agendamentoAtualizado = agendamentoLockService.atualizarDadosPix(agendamentoSalvo.getId_agendamento(), pixDados);

        return AgendamentoResponseDTO.fromEntity(agendamentoAtualizado);
    }

    @Transactional
    public AgendamentoResponseDTO confirmarPagamento(Long idAgendamento, Long usuarioIdAutenticado) {
        Agendamento agendamento = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado. ID: " + idAgendamento));

        Usuario usuarioAutenticado = usuarioRepository.findById(usuarioIdAutenticado).orElse(null);
        boolean ehMasterAdmin = usuarioAutenticado != null && usuarioAutenticado.isMasterAdmin();
        boolean ehDono = agendamento.getUsuario().getId_usuario().equals(usuarioIdAutenticado);
        boolean ehAdminDaQuadra = agendamento.getQuadra().getAdmin() != null
                && agendamento.getQuadra().getAdmin().getId_usuario().equals(usuarioIdAutenticado);
        if (!ehDono && !ehAdminDaQuadra && !ehMasterAdmin) {
            throw new RegraNegocioException("ACESSO_NEGADO", "Você não tem permissão para confirmar o pagamento deste agendamento.");
        }

        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            return AgendamentoResponseDTO.fromEntity(agendamento);
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("STATUS_INVALIDO", "Não é possível confirmar pagamento de um agendamento cancelado.");
        }

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new AgendamentoPagamentoConfirmadoEvent(salvo));
        }

        return AgendamentoResponseDTO.fromEntity(salvo);
    }

    @Transactional
    public AgendamentoResponseDTO confirmarPagamentoPorWebhook(Long idAgendamento, String transacaoId) {
        return confirmarPagamentoPorWebhook(idAgendamento, transacaoId, null);
    }

    @Transactional
    public AgendamentoResponseDTO confirmarPagamentoPorWebhook(Long idAgendamento, String transacaoId, BigDecimal valorPago) {
        Agendamento agendamento = null;
        if (idAgendamento != null) {
            agendamento = agendamentoRepository.findById(idAgendamento).orElse(null);
        }
        if (agendamento == null && transacaoId != null && !transacaoId.isBlank()) {
            agendamento = agendamentoRepository.findByTransacaoPagamentoId(transacaoId).orElse(null);
        }

        if (agendamento == null) {
            throw new RecursoNaoEncontradoException("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado para conciliação do pagamento (ID: "
                    + idAgendamento + ", transacaoId: " + transacaoId + ")");
        }

        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            return AgendamentoResponseDTO.fromEntity(agendamento);
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            log.error("ALERTA CRÍTICO: Pagamento recebido para agendamento {} que já estava CANCELADO. Necessário estorno!", agendamento.getId_agendamento());
            throw new RegraNegocioException("STATUS_INVALIDO", "Não é possível confirmar pagamento de um agendamento cancelado. Favor estornar o valor ao cliente.");
        }

        if (valorPago != null && valorPago.compareTo(agendamento.getValorTotal()) < 0) {
            log.error("Valor pago no gateway [{}] é inferior ao valor total [{}] da reserva {}", valorPago, agendamento.getValorTotal(), agendamento.getId_agendamento());
            throw new RegraNegocioException("VALOR_INCONSISTENTE", "Valor pago inconsistente com o valor contratado da reserva.");
        }

        int afetados = agendamentoRepository.confirmarPagamentoPendente(
                agendamento.getId_agendamento(),
                transacaoId,
                StatusAgendamento.CONFIRMADO,
                StatusAgendamento.PENDENTE
        );

        if (afetados == 0) {
            log.error("ALERTA CRÍTICO: Conflito de concorrência. Agendamento {} não estava mais PENDENTE no momento da confirmação atômica.", agendamento.getId_agendamento());
            throw new RegraNegocioException("CONFLITO_STATUS", "Não foi possível confirmar o agendamento pois ele foi expirado ou cancelado concorrentemente.");
        }

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        if (transacaoId != null && !transacaoId.isBlank()) {
            agendamento.setTransacaoPagamentoId(transacaoId);
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new AgendamentoPagamentoConfirmadoEvent(agendamento));
        }

        return AgendamentoResponseDTO.fromEntity(agendamento);
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarPorId(Long idAgendamento, Long usuarioIdAutenticado) {
        Usuario usuarioAutenticado = usuarioRepository.findById(usuarioIdAutenticado).orElse(null);
        boolean ehMasterAdmin = usuarioAutenticado != null && usuarioAutenticado.isMasterAdmin();

        Agendamento agendamento;
        if (ehMasterAdmin) {
            agendamento = agendamentoRepository.findById(idAgendamento)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado. ID: " + idAgendamento));
        } else {
            agendamento = agendamentoRepository.buscarPorIdEEscopo(idAgendamento, usuarioIdAutenticado)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado. ID: " + idAgendamento));
        }

        return AgendamentoResponseDTO.fromEntity(agendamento);
    }

    @Transactional
    public AgendamentoResponseDTO cancelar(Long id, Long usuarioId) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("AGENDAMENTO_NAO_ENCONTRADO", "Agendamento não encontrado para o ID: " + id));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (usuario.isMasterAdmin()) {
            // Master Admin tem permissão para cancelar qualquer agendamento
        } else if (usuario.getRole() == com.agendamentos.equadras.model.enums.Role.CLIENT) {
            if (!agendamento.getUsuario().getId_usuario().equals(usuarioId)) {
                throw new org.springframework.security.access.AccessDeniedException("Você não tem permissão para cancelar este agendamento.");
            }
        } else if (usuario.getRole() == com.agendamentos.equadras.model.enums.Role.ADMIN) {
            if (!agendamento.getQuadra().getAdmin().getId_usuario().equals(usuarioId)) {
                throw new org.springframework.security.access.AccessDeniedException("Você não tem permissão para cancelar agendamentos desta quadra.");
            }
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalArgumentException("Este agendamento já está cancelado.");
        }

        if (!agendamento.getDataHoraInicio().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Não é possível cancelar um agendamento que está em andamento ou retroativo.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setCanceladoEm(LocalDateTime.now(clock));
        Agendamento agendamentoAtualizado = agendamentoRepository.save(agendamento);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new AgendamentoCanceladoEvent(agendamentoAtualizado, usuario));
        }

        return AgendamentoResponseDTO.fromEntity(agendamentoAtualizado);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarPorQuadraEData(Long quadraId, LocalDate data) {
        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.atTime(LocalTime.MAX);

        return agendamentoRepository
                .buscarPorQuadraEData(
                        quadraId,
                        StatusAgendamento.CANCELADO,
                        inicioDoDia,
                        fimDoDia
                )
                .stream()
                .map(AgendamentoResponseDTO::fromEntitySemPix)
                .toList();
    }

    private void validarAcessoQuadra(Quadra quadra, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        boolean ehMasterAdmin = usuario.isMasterAdmin();
        boolean ehDonoQuadra = quadra.getAdmin() != null && quadra.getAdmin().getId_usuario().equals(usuarioId);

        if (!ehMasterAdmin && !ehDonoQuadra) {
            throw new org.springframework.security.access.AccessDeniedException("Você não tem permissão para visualizar o histórico desta quadra.");
        }
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
    public List<AgendamentoResponseDTO> listarPorQuadra(Long quadraId, Long usuarioId) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        validarAcessoQuadra(quadra, usuarioId);

        return agendamentoRepository.findByQuadraIdOrderByDataHoraInicioDesc(quadraId)
                .stream()
                .map(AgendamentoResponseDTO::fromEntitySemPix)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AgendamentoResponseDTO> listarPorQuadraPaginado(Long quadraId, AbaAgendamento aba, Pageable pageable, Long usuarioId) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        validarAcessoQuadra(quadra, usuarioId);

        LocalDateTime agora = LocalDateTime.now(clock);
        Specification<Agendamento> spec = AgendamentoSpecifications.daQuadra(quadraId);
        if (aba != null) {
            spec = spec.and(AgendamentoSpecifications.daAba(aba, agora));
        }

        SortPolicy policy = (aba == AbaAgendamento.ATIVOS) ? SORT_POLICY_ATIVOS : SORT_POLICY_DESC;
        Pageable pageableComSort = policy.apply(pageable);

        Page<Agendamento> pagina = agendamentoRepository.findAll(spec, pageableComSort);

        return PageResponse.of(pagina, AgendamentoResponseDTO::fromEntitySemPix);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> contarPorAbaEQuadra(Long quadraId, Long usuarioId) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        validarAcessoQuadra(quadra, usuarioId);

        LocalDateTime agora = LocalDateTime.now(clock);
        Specification<Agendamento> base = AgendamentoSpecifications.daQuadra(quadraId);

        Map<String, Long> contagens = new java.util.LinkedHashMap<>();
        contagens.put("TODOS", agendamentoRepository.count(base));
        for (AbaAgendamento aba : AbaAgendamento.values()) {
            contagens.put(aba.name(), agendamentoRepository.count(base.and(AgendamentoSpecifications.daAba(aba, agora))));
        }
        return contagens;
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
        java.time.YearMonth anoMes = java.time.YearMonth.of(ano, mes);
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

    @Transactional(readOnly = true)
    public List<HorarioDisponivelDTO> listarHorariosDisponiveis(Long quadraId, LocalDate data) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new IllegalArgumentException("Quadra não encontrada para o ID: " + quadraId));

        java.time.DayOfWeek diaSemana = data.getDayOfWeek();
        com.agendamentos.equadras.model.entity.DisponibilidadeDia disp = null;
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

        List<com.agendamentos.equadras.model.entity.BloqueioHorario> bloqueios = bloqueioHorarioRepository.findByQuadraIdAndData(quadraId, data);
        return montarSlotsHorarios(quadra, data, agendamentosDoDia, bloqueios);
    }

    private Specification<Agendamento> escopoListagemPorPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (usuario.getRole() == Role.ADMIN) {
            return usuario.isMasterAdmin()
                    ? (root, query, cb) -> cb.conjunction()
                    : AgendamentoSpecifications.doAdmin(usuarioId);
        }
        return AgendamentoSpecifications.doUsuario(usuarioId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgendamentoResponseDTO> listarPaginado(Long usuarioId, AbaAgendamento aba, boolean apenasPendentes, Pageable pageable) {
        if (!apenasPendentes && aba == null) {
            throw new IllegalArgumentException("Aba de agendamento é obrigatória.");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        Specification<Agendamento> spec = escopoListagemPorPerfil(usuarioId);

        if (apenasPendentes) {
            spec = spec.and(AgendamentoSpecifications.apenasPendentesValidos(agora));
        } else {
            spec = spec.and(AgendamentoSpecifications.daAba(aba, agora));
        }

        SortPolicy policy = (aba == AbaAgendamento.ATIVOS || apenasPendentes) ? SORT_POLICY_ATIVOS : SORT_POLICY_DESC;
        Pageable pageableComSort = policy.apply(pageable);

        Page<Agendamento> pagina = agendamentoRepository.findAll(spec, pageableComSort);

        return PageResponse.of(pagina, a -> {
            if (a.getStatus() == StatusAgendamento.PENDENTE && a.getUsuario() != null && usuarioId.equals(a.getUsuario().getId_usuario())) {
                return AgendamentoResponseDTO.fromEntity(a);
            }
            return AgendamentoResponseDTO.fromEntitySemPix(a);
        });
    }

    @Transactional(readOnly = true)
    public Map<AbaAgendamento, Long> contarPorAba(Long usuarioId) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Specification<Agendamento> base = escopoListagemPorPerfil(usuarioId);
        Map<AbaAgendamento, Long> contagens = new EnumMap<>(AbaAgendamento.class);
        for (AbaAgendamento aba : AbaAgendamento.values()) {
            contagens.put(aba, agendamentoRepository.count(base.and(AgendamentoSpecifications.daAba(aba, agora))));
        }
        return contagens;
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarTodos(Long usuarioId) {
        return listarTodos(usuarioId, false);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarTodos(Long usuarioId, boolean historico) {
        List<Agendamento> agendamentos;
        Usuario usuario = (usuarioId != null) ? usuarioRepository.findById(usuarioId).orElse(null) : null;

        if (usuario != null) {
            if (usuario.getRole() == com.agendamentos.equadras.model.enums.Role.ADMIN) {
                if (usuario.isMasterAdmin()) {
                    // Master Admin vê todos os agendamentos (histórico completo ou apenas ativos)
                    if (historico) {
                        agendamentos = agendamentoRepository.findAllOrderByDataHoraInicioDesc();
                    } else {
                        agendamentos = agendamentoRepository.findAtivosAll(
                                StatusAgendamento.CANCELADO,
                                LocalDateTime.now(clock)
                        );
                    }
                } else {
                    // Admin comum vê os agendamentos das suas quadras (histórico completo ou apenas ativos)
                    if (historico) {
                        agendamentos = agendamentoRepository.findByAdminId(usuarioId);
                    } else {
                        agendamentos = agendamentoRepository.findAtivosByAdminId(
                                usuarioId,
                                StatusAgendamento.CANCELADO,
                                LocalDateTime.now(clock)
                        );
                    }
                }
            } else if (usuario != null && usuario.getRole() == com.agendamentos.equadras.model.enums.Role.CLIENT) {
                if (historico) {
                    agendamentos = agendamentoRepository.findByUsuarioId(usuarioId);
                } else {
                    agendamentos = agendamentoRepository.findAtivosByUsuarioId(
                            usuarioId,
                            StatusAgendamento.CANCELADO,
                            LocalDateTime.now(clock)
                    );
                }
            } else {
                agendamentos = agendamentoRepository.findAllOrderByDataHoraInicioDesc();
            }
        } else {
            agendamentos = agendamentoRepository.findAllOrderByDataHoraInicioDesc();
        }

        return agendamentos.stream()
                .map(a -> {
                    // Mantém dados Pix para agendamentos pendentes do próprio cliente para pagamento imediato
                    if (a.getStatus() == StatusAgendamento.PENDENTE && usuario != null && usuario.getId_usuario().equals(a.getUsuario().getId_usuario())) {
                        return AgendamentoResponseDTO.fromEntity(a);
                    }
                    return AgendamentoResponseDTO.fromEntitySemPix(a);
                })
                .toList();
    }

    private List<HorarioDisponivelDTO> montarSlotsHorarios(
            Quadra quadra,
            LocalDate data,
            List<Agendamento> agendamentosDoDia,
            List<com.agendamentos.equadras.model.entity.BloqueioHorario> bloqueios) {

        java.time.DayOfWeek diaSemana = data.getDayOfWeek();
        com.agendamentos.equadras.model.entity.DisponibilidadeDia disp = null;
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
        com.agendamentos.equadras.model.entity.BloqueioHorario bloqueioDiaInteiro = bloqueios.stream()
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
            com.agendamentos.equadras.model.enums.StatusHorario status = com.agendamentos.equadras.model.enums.StatusHorario.DISPONIVEL;

            if (!quadra.isAtiva()) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.INDISPONIVEL;
                motivo = "Quadra inativa";
            } else if (dataLimiteExcedida) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO;
                motivo = "Data limite de agendamento encerrada";
            } else if (bloqueioDiaInteiro != null) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO;
                motivo = (bloqueioDiaInteiro.getMotivo() != null && !bloqueioDiaInteiro.getMotivo().isBlank())
                        ? "Bloqueado: " + bloqueioDiaInteiro.getMotivo()
                        : "Horário bloqueado pelo administrador";
            } else if (slotDataHoraInicio.isBefore(agora)) {
                disponivel = false;
                status = com.agendamentos.equadras.model.enums.StatusHorario.INDISPONIVEL;
                motivo = "Horário indisponível (passado)";
            } else {
                final LocalTime sIni = slotInicio;
                final LocalTime sFim = slotFim;
                com.agendamentos.equadras.model.entity.BloqueioHorario bloqueioParcial = bloqueios.stream()
                        .filter(b -> b.getHoraInicio() != null && b.getHoraFim() != null)
                        .filter(b -> b.getHoraInicio().isBefore(sFim) && b.getHoraFim().isAfter(sIni))
                        .findFirst()
                        .orElse(null);

                if (bloqueioParcial != null) {
                    disponivel = false;
                    status = com.agendamentos.equadras.model.enums.StatusHorario.BLOQUEADO;
                    motivo = (bloqueioParcial.getMotivo() != null && !bloqueioParcial.getMotivo().isBlank())
                            ? "Bloqueado: " + bloqueioParcial.getMotivo()
                            : "Horário bloqueado pelo administrador";
                } else {
                    boolean ocupado = agendamentosDoDia.stream().anyMatch(a ->
                            a.getDataHoraInicio().isBefore(slotDataHoraFim) && a.getDataHoraFim().isAfter(slotDataHoraInicio)
                    );
                    if (ocupado) {
                        disponivel = false;
                        status = com.agendamentos.equadras.model.enums.StatusHorario.AGENDADO;
                        motivo = "Horário ocupado / agendado";
                    }
                }
            }

            slots.add(new HorarioDisponivelDTO(slotInicio, slotFim, disponivel, status, motivo));
            slotInicio = slotFim;
        }

        return slots;
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, List<HorarioDisponivelDTO>> listarHorariosDoDiaParaAdmin(LocalDate data, Long adminId) {
        Usuario admin = usuarioRepository.findById(adminId).orElse(null);
        List<Quadra> quadrasDoAdmin;
        if (admin != null && admin.isMasterAdmin()) {
            quadrasDoAdmin = quadraRepository.findAllWithAdminEFotos();
        } else {
            quadrasDoAdmin = quadraRepository.findByAdminId(adminId);
        }

        java.util.Map<Long, List<HorarioDisponivelDTO>> mapaResultado = new java.util.LinkedHashMap<>();
        List<Quadra> quadrasAtivas = quadrasDoAdmin.stream().filter(Quadra::isAtiva).toList();

        if (quadrasAtivas.isEmpty()) {
            for (Quadra quadra : quadrasDoAdmin) {
                mapaResultado.put(quadra.getId_quadra(), List.of());
            }
            return mapaResultado;
        }

        List<Long> quadraIdsAtivas = quadrasAtivas.stream().map(Quadra::getId_quadra).toList();
        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.atTime(LocalTime.MAX);

        List<Agendamento> agendamentosEmLote = agendamentoRepository.buscarPorQuadrasEDataLote(
                quadraIdsAtivas,
                StatusAgendamento.CANCELADO,
                inicioDoDia,
                fimDoDia
        );

        List<com.agendamentos.equadras.model.entity.BloqueioHorario> bloqueiosEmLote =
                bloqueioHorarioRepository.findByQuadraIdsAndData(quadraIdsAtivas, data);

        java.util.Map<Long, List<Agendamento>> agendamentosPorQuadra = agendamentosEmLote.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getQuadra().getId_quadra()));

        java.util.Map<Long, List<com.agendamentos.equadras.model.entity.BloqueioHorario>> bloqueiosPorQuadra = bloqueiosEmLote.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> b.getQuadra().getId_quadra()));

        for (Quadra quadra : quadrasDoAdmin) {
            if (quadra.isAtiva()) {
                List<Agendamento> agendamentosQuadra = agendamentosPorQuadra.getOrDefault(quadra.getId_quadra(), List.of());
                List<com.agendamentos.equadras.model.entity.BloqueioHorario> bloqueiosQuadra = bloqueiosPorQuadra.getOrDefault(quadra.getId_quadra(), List.of());
                List<HorarioDisponivelDTO> slots = montarSlotsHorarios(quadra, data, agendamentosQuadra, bloqueiosQuadra);
                mapaResultado.put(quadra.getId_quadra(), slots);
            } else {
                mapaResultado.put(quadra.getId_quadra(), List.of());
            }
        }

        return mapaResultado;
    }

    @Transactional(readOnly = true)
    public DashboardMetricasDTO obterMetricasDashboard(Long adminId) {
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Usuário não encontrado."));

        if (admin.getRole() != Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas administradores podem acessar as métricas do painel.");
        }

        boolean ehMasterAdmin = admin.isMasterAdmin();

        LocalDate hoje = LocalDate.now(clock);
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime inicioAmanha = hoje.plusDays(1).atStartOfDay();

        List<Object[]> quadrasResult = ehMasterAdmin
                ? quadraRepository.obterMetricasQuadrasMasterAdmin()
                : quadraRepository.obterMetricasQuadrasPorAdminId(adminId);

        Object[] rowQuadra = (quadrasResult != null && !quadrasResult.isEmpty())
                ? quadrasResult.get(0)
                : new Object[]{0L, 0L};

        long totalQuadras = rowQuadra[0] != null ? ((Number) rowQuadra[0]).longValue() : 0L;
        long quadrasAtivas = rowQuadra[1] != null ? ((Number) rowQuadra[1]).longValue() : 0L;

        List<Object[]> agendamentosResult = ehMasterAdmin
                ? agendamentoRepository.obterMetricasAgendamentosMasterAdmin(inicioHoje, inicioAmanha)
                : agendamentoRepository.obterMetricasAgendamentosPorAdminId(adminId, inicioHoje, inicioAmanha);

        Object[] rowAgendamento = (agendamentosResult != null && !agendamentosResult.isEmpty())
                ? agendamentosResult.get(0)
                : new Object[]{0L, BigDecimal.ZERO, 0L};

        long totalReservas = rowAgendamento[0] != null ? ((Number) rowAgendamento[0]).longValue() : 0L;
        BigDecimal faturamentoTotal = rowAgendamento[1] != null ? (BigDecimal) rowAgendamento[1] : BigDecimal.ZERO;
        long reservasHoje = rowAgendamento[2] != null ? ((Number) rowAgendamento[2]).longValue() : 0L;

        return new DashboardMetricasDTO(
                totalQuadras,
                quadrasAtivas,
                totalReservas,
                faturamentoTotal,
                reservasHoje
        );
    }

    @Transactional(readOnly = true)
    public List<com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO> consultarGradeHorarios(
            LocalDate data, Long quadraId, String tipoEsporte, String nomeQuadra, boolean apenasDisponiveis) {
        
        List<Quadra> quadras = quadraService.filtrarQuadrasEntidades(null, null, null, null, tipoEsporte, nomeQuadra, null, null, null);
        if (quadraId != null) {
            quadras = quadras.stream().filter(q -> q.getId_quadra().equals(quadraId)).toList();
        }

        List<com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO> resultado = new ArrayList<>();
        for (Quadra q : quadras) {
            List<HorarioDisponivelDTO> slots = listarHorariosDisponiveis(q.getId_quadra(), data);
            if (apenasDisponiveis) {
                slots = slots.stream().filter(HorarioDisponivelDTO::disponivel).toList();
            }
            if (!slots.isEmpty() || !apenasDisponiveis) {
                resultado.add(new com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO(
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
    public List<com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO> consultarGradeHorariosFlexivel(
            String dataFlexivel, Long quadraId, String tipoEsporte, String nomeQuadra, boolean apenasDisponiveis) {
        LocalDate dataResolvida = com.agendamentos.equadras.util.DataFlexivelUtil.resolverData(dataFlexivel);
        
        if (dataResolvida != null) {
            return consultarGradeHorarios(dataResolvida, quadraId, tipoEsporte, nomeQuadra, apenasDisponiveis);
        }

        // Predição de 14 dias para encontrar o próximo dia com horários disponíveis
        LocalDate inicio = LocalDate.now(clock);
        List<com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO> resultadoFinal = new ArrayList<>();
        
        for (int i = 0; i < 14; i++) {
            LocalDate dataAlvo = inicio.plusDays(i);
            List<com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO> gradeDia = 
                    consultarGradeHorarios(dataAlvo, quadraId, tipoEsporte, nomeQuadra, apenasDisponiveis);
            
            if (!gradeDia.isEmpty()) {
                resultadoFinal.addAll(gradeDia);
                // Retorna apenas os horários do primeiro dia que tiver disponibilidade
                return resultadoFinal;
            }
        }
        
        return resultadoFinal;
    }

    public AgendamentoResponseDTO agendarViaBot(com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO dto) {
        Long quadraId = dto.quadraId();
        if (quadraId == null) {
            List<Quadra> quadras = quadraService.filtrarQuadrasEntidades(null, null, null, null, dto.tipoEsporte(), dto.nomeQuadra(), null, null, null);
            if (quadras.isEmpty()) {
                throw new IllegalArgumentException("Nenhuma quadra encontrada para o esporte ou nome informado.");
            }
            quadraId = quadras.get(0).getId_quadra();
        }

        LocalDate data = com.agendamentos.equadras.util.DataFlexivelUtil.resolverData(dto.data());
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

        return agendar(criacaoDTO, usuario.getId_usuario());
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

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    @Transactional
    public void expirarAgendamentosPendentes() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.minusMinutes(15);
        int cancelados = agendamentoRepository.cancelarPendentesExpirados(
                StatusAgendamento.PENDENTE,
                StatusAgendamento.CANCELADO,
                limite,
                agora
        );
        if (cancelados > 0 && eventPublisher != null) {
            eventPublisher.publishEvent(new AgendamentosExpiradosCanceladosEvent(cancelados));
        }
    }
}