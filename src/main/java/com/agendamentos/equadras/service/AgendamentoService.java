package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.model.entity.Agendamento;
import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.model.enums.TipoExecutor;
import com.agendamentos.equadras.event.AgendamentoCanceladoEvent;
import com.agendamentos.equadras.event.AgendamentoNotificacaoPayload;
import com.agendamentos.equadras.event.AgendamentoPagamentoConfirmadoEvent;
import com.agendamentos.equadras.model.enums.StatusAgendamento;
import com.agendamentos.equadras.exception.RecursoNaoEncontradoException;
import com.agendamentos.equadras.exception.RegraNegocioException;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.ContagemPorAba;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final UsuarioService usuarioService;
    private final QuadraRepository quadraRepository;
    private final PagamentoService pagamentoService;
    private final AgendamentoLockService agendamentoLockService;
    private final ApplicationEventPublisher eventPublisher;
    private final java.time.Clock clock;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              UsuarioService usuarioService,
                              QuadraRepository quadraRepository,
                              PagamentoService pagamentoService,
                              AgendamentoLockService agendamentoLockService,
                              ApplicationEventPublisher eventPublisher,
                              java.time.Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioService = usuarioService;
        this.quadraRepository = quadraRepository;
        this.pagamentoService = pagamentoService;
        this.agendamentoLockService = agendamentoLockService;
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

        boolean ehDono = agendamento.getUsuario().getId_usuario().equals(usuarioIdAutenticado);
        if (!ehDono && !usuarioService.podeGerenciarQuadra(agendamento.getQuadra(), usuarioIdAutenticado)) {
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
            eventPublisher.publishEvent(new AgendamentoPagamentoConfirmadoEvent(AgendamentoNotificacaoPayload.fromEntity(salvo)));
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
            eventPublisher.publishEvent(new AgendamentoPagamentoConfirmadoEvent(AgendamentoNotificacaoPayload.fromEntity(agendamento)));
        }

        return AgendamentoResponseDTO.fromEntity(agendamento);
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarPorId(Long idAgendamento, Long usuarioIdAutenticado) {
        Usuario usuarioAutenticado = usuarioService.buscarPorIdEntidade(usuarioIdAutenticado).orElse(null);
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

        Usuario usuario = usuarioService.buscarPorIdEntidade(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (usuario.isMasterAdmin()) {
            // Master Admin tem permissão para cancelar qualquer agendamento
        } else if (usuario.getRole() == Role.CLIENT) {
            if (!agendamento.getUsuario().getId_usuario().equals(usuarioId)) {
                throw new AccessDeniedException("Você não tem permissão para cancelar este agendamento.");
            }
        } else if (usuario.getRole() == Role.ADMIN) {
            if (!usuarioService.podeGerenciarQuadra(agendamento.getQuadra(), usuarioId)) {
                throw new AccessDeniedException("Você não tem permissão para cancelar agendamentos desta quadra.");
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
            // usuario nunca é nulo aqui: a busca acima lança exceção quando não encontrado
            String nomeExecutor = usuario.getNome_usuario() != null ? usuario.getNome_usuario() : "Sistema";
            TipoExecutor tipoExec = usuario.isMasterAdmin()
                    ? TipoExecutor.MASTER_ADMIN
                    : (usuario.getRole() == Role.ADMIN ? TipoExecutor.ADMIN_QUADRA : TipoExecutor.CLIENTE);

            eventPublisher.publishEvent(new AgendamentoCanceladoEvent(
                    AgendamentoNotificacaoPayload.fromEntity(agendamentoAtualizado),
                    usuario.getId_usuario(),
                    usuario.getEmail_usuario(),
                    nomeExecutor,
                    tipoExec
            ));
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
        if (usuarioService.buscarPorIdEntidade(usuarioId).isEmpty()) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        if (!usuarioService.podeGerenciarQuadra(quadra, usuarioId)) {
            throw new AccessDeniedException("Você não tem permissão para visualizar o histórico desta quadra.");
        }
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
    public boolean possuiAgendamentos(Long quadraId) {
        if (quadraId == null) return false;
        return agendamentoRepository.existsByQuadraId(quadraId);
    }

    @Transactional(readOnly = true)
    public boolean existeConflitoHorario(Long quadraId, LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.existeConflitoHorario(
                quadraId,
                inicio,
                fim,
                com.agendamentos.equadras.model.enums.StatusAgendamento.CANCELADO
        );
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

        ContagemPorAba contagem = agendamentoRepository.contarPorAba(base, agora);
        Map<String, Long> contagens = new java.util.LinkedHashMap<>();
        contagens.put("TODOS", contagem.total());
        for (AbaAgendamento aba : AbaAgendamento.values()) {
            contagens.put(aba.name(), contagem.porAba().get(aba));
        }
        return contagens;
    }





    private Specification<Agendamento> escopoListagemPorPerfil(Long usuarioId) {
        Usuario usuario = usuarioService.buscarPorIdEntidade(usuarioId)
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
        return new EnumMap<>(agendamentoRepository.contarPorAba(base, agora).porAba());
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarTodos(Long usuarioId) {
        return listarTodos(usuarioId, false);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarTodos(Long usuarioId, boolean historico) {
        List<Agendamento> agendamentos;
        Usuario usuario = (usuarioId != null) ? usuarioService.buscarPorIdEntidade(usuarioId).orElse(null) : null;

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
                agendamentos = List.of();
            }
        } else {
            agendamentos = List.of();
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




}