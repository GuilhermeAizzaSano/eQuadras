package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.DashboardMetricasDTO;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.AgendamentoRepository;
import com.agendamentos.equadras.repository.QuadraRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    private final UsuarioService usuarioService;
    private final QuadraRepository quadraRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final Clock clock;

    public DashboardService(UsuarioService usuarioService,
                            QuadraRepository quadraRepository,
                            AgendamentoRepository agendamentoRepository,
                            Clock clock) {
        this.usuarioService = usuarioService;
        this.quadraRepository = quadraRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardMetricasDTO obterMetricasDashboard(Long adminId) {
        if (adminId == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        Usuario admin = usuarioService.buscarPorIdEntidade(adminId)
                .orElseThrow(() -> new AccessDeniedException("Usuário não encontrado."));

        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas administradores podem acessar as métricas do painel.");
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
}
