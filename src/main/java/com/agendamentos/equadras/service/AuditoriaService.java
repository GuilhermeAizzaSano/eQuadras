package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.response.EstatisticasAuditoriaDTO;
import com.agendamentos.equadras.dto.response.LogAuditoriaResponseDTO;
import com.agendamentos.equadras.model.entity.LogAuditoria;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.CategoriaAuditoria;
import com.agendamentos.equadras.model.enums.TipoExecutor;
import com.agendamentos.equadras.repository.LogAuditoriaRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import com.agendamentos.equadras.util.DataFlexivelUtil;
import com.agendamentos.equadras.util.HttpRequestUtil;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(LogAuditoriaRepository logAuditoriaRepository, UsuarioRepository usuarioRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private TipoExecutor resolverTipoExecutor(Usuario usuario) {
        if (usuario == null) return TipoExecutor.SISTEMA;
        if (usuario.isMasterAdmin()) return TipoExecutor.MASTER_ADMIN;
        if (usuario.getRole() == com.agendamentos.equadras.model.enums.Role.ADMIN) return TipoExecutor.ADMIN_QUADRA;
        return TipoExecutor.CLIENTE;
    }

    public void registrarLoginSucesso(Usuario usuario) {
        registrarLoginSucesso(usuario, HttpRequestUtil.extrairClientIp(null), HttpRequestUtil.extrairUserAgent(null));
    }

    public void registrarLoginFalha(String emailInformado, String motivo) {
        registrarLoginFalha(emailInformado, motivo, HttpRequestUtil.extrairClientIp(null), HttpRequestUtil.extrairUserAgent(null));
    }

    public void registrarLogout(Usuario usuario) {
        registrarLogout(usuario, HttpRequestUtil.extrairClientIp(null), HttpRequestUtil.extrairUserAgent(null));
    }

    public void registrarAlteracaoSenha(Usuario usuario) {
        registrarAlteracaoSenha(usuario, HttpRequestUtil.extrairClientIp(null), HttpRequestUtil.extrairUserAgent(null));
    }

    public void registrarAcao(Usuario usuario, CategoriaAuditoria categoria, String acao,
                              String entidade, String recursoId, String detalhes) {
        registrarAcao(usuario, categoria, acao, entidade, recursoId, detalhes,
                HttpRequestUtil.extrairClientIp(null), HttpRequestUtil.extrairUserAgent(null));
    }

    public void registrarAcaoPorUsuarioId(Long usuarioId, CategoriaAuditoria categoria, String acao,
                                         String entidade, String recursoId, String detalhes) {
        registrarAcaoPorUsuarioId(usuarioId, categoria, acao, entidade, recursoId, detalhes,
                HttpRequestUtil.extrairClientIp(null), HttpRequestUtil.extrairUserAgent(null));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLoginSucesso(Usuario usuario, String ip, String userAgent) {
        try {
            LogAuditoria entrada = LogAuditoria.builder()
                    .usuarioId(usuario != null ? usuario.getId_usuario() : null)
                    .usuarioEmail(usuario != null ? usuario.getEmail_usuario() : null)
                    .usuarioNome(usuario != null ? usuario.getNome_usuario() : null)
                    .categoria(CategoriaAuditoria.AUTENTICACAO)
                    .acao("LOGIN_SUCESSO")
                    .entidade("USUARIO")
                    .recursoId(usuario != null && usuario.getId_usuario() != null ? usuario.getId_usuario().toString() : null)
                    .tipoExecutor(resolverTipoExecutor(usuario))
                    .detalhes("Login realizado com sucesso. Sessão HttpOnly emitida.")
                    .ip(ip)
                    .userAgent(userAgent)
                    .criadoEm(Instant.now())
                    .build();

            logAuditoriaRepository.save(entrada);
        } catch (Exception e) {
            log.warn("Falha defensiva ao registrar auditoria de LOGIN_SUCESSO: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLoginFalha(String emailInformado, String motivo, String ip, String userAgent) {
        try {
            LogAuditoria entrada = LogAuditoria.builder()
                    .usuarioId(null)
                    .usuarioEmail(emailInformado)
                    .usuarioNome(null)
                    .categoria(CategoriaAuditoria.AUTENTICACAO)
                    .acao("LOGIN_FALHA")
                    .entidade("USUARIO")
                    .recursoId(null)
                    .tipoExecutor(TipoExecutor.CLIENTE)
                    .detalhes("Falha na tentativa de autenticação: " + motivo)
                    .ip(ip)
                    .userAgent(userAgent)
                    .criadoEm(Instant.now())
                    .build();

            logAuditoriaRepository.save(entrada);
        } catch (Exception e) {
            log.warn("Falha defensiva ao registrar auditoria de LOGIN_FALHA: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLogout(Usuario usuario, String ip, String userAgent) {
        try {
            LogAuditoria entrada = LogAuditoria.builder()
                    .usuarioId(usuario != null ? usuario.getId_usuario() : null)
                    .usuarioEmail(usuario != null ? usuario.getEmail_usuario() : null)
                    .usuarioNome(usuario != null ? usuario.getNome_usuario() : null)
                    .categoria(CategoriaAuditoria.AUTENTICACAO)
                    .acao("LOGOUT")
                    .entidade("USUARIO")
                    .recursoId(usuario != null && usuario.getId_usuario() != null ? usuario.getId_usuario().toString() : null)
                    .tipoExecutor(resolverTipoExecutor(usuario))
                    .detalhes("Logout efetuado com sucesso. Cookie de sessão invalidado.")
                    .ip(ip)
                    .userAgent(userAgent)
                    .criadoEm(Instant.now())
                    .build();

            logAuditoriaRepository.save(entrada);
        } catch (Exception e) {
            log.warn("Falha defensiva ao registrar auditoria de LOGOUT: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAlteracaoSenha(Usuario usuario, String ip, String userAgent) {
        try {
            LogAuditoria entrada = LogAuditoria.builder()
                    .usuarioId(usuario != null ? usuario.getId_usuario() : null)
                    .usuarioEmail(usuario != null ? usuario.getEmail_usuario() : null)
                    .usuarioNome(usuario != null ? usuario.getNome_usuario() : null)
                    .categoria(CategoriaAuditoria.AUTENTICACAO)
                    .acao("ALTERAR_SENHA")
                    .entidade("USUARIO")
                    .recursoId(usuario != null && usuario.getId_usuario() != null ? usuario.getId_usuario().toString() : null)
                    .tipoExecutor(resolverTipoExecutor(usuario))
                    .detalhes("Senha alterada pelo próprio usuário.")
                    .ip(ip)
                    .userAgent(userAgent)
                    .criadoEm(Instant.now())
                    .build();

            logAuditoriaRepository.save(entrada);
        } catch (Exception e) {
            log.warn("Falha defensiva ao registrar auditoria de ALTERAR_SENHA: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcao(Usuario usuario, CategoriaAuditoria categoria, String acao,
                              String entidade, String recursoId, String detalhes,
                              String ip, String userAgent) {
        try {
            LogAuditoria entrada = LogAuditoria.builder()
                    .usuarioId(usuario != null ? usuario.getId_usuario() : null)
                    .usuarioEmail(usuario != null ? usuario.getEmail_usuario() : null)
                    .usuarioNome(usuario != null ? usuario.getNome_usuario() : null)
                    .categoria(categoria)
                    .acao(acao)
                    .entidade(entidade)
                    .recursoId(recursoId)
                    .tipoExecutor(resolverTipoExecutor(usuario))
                    .detalhes(detalhes)
                    .ip(ip)
                    .userAgent(userAgent)
                    .criadoEm(Instant.now())
                    .build();

            logAuditoriaRepository.save(entrada);
        } catch (Exception e) {
            log.warn("Falha defensiva ao registrar auditoria acao={} entidade={}: {}", acao, entidade, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcaoPorUsuarioId(Long usuarioId, CategoriaAuditoria categoria, String acao,
                                         String entidade, String recursoId, String detalhes,
                                         String ip, String userAgent) {
        Usuario usuario = null;
        if (usuarioId != null) {
            usuario = usuarioRepository.findById(usuarioId).orElse(null);
        }
        registrarAcao(usuario, categoria, acao, entidade, recursoId, detalhes, ip, userAgent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcaoSistema(CategoriaAuditoria categoria, String acao,
                                     String entidade, String recursoId, String detalhes) {
        try {
            LogAuditoria entrada = LogAuditoria.builder()
                    .usuarioId(null)
                    .usuarioEmail("sistema@equadras.internal")
                    .usuarioNome("Processo do Sistema")
                    .categoria(categoria)
                    .acao(acao)
                    .entidade(entidade)
                    .recursoId(recursoId)
                    .tipoExecutor(TipoExecutor.SISTEMA)
                    .detalhes(detalhes)
                    .ip("127.0.0.1")
                    .userAgent("ScheduledTask/Background")
                    .criadoEm(Instant.now())
                    .build();

            logAuditoriaRepository.save(entrada);
        } catch (Exception e) {
            log.warn("Falha defensiva ao registrar auditoria de sistema acao={}: {}", acao, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<LogAuditoriaResponseDTO> listarLogs(Pageable pageable,
                                                   Long usuarioId,
                                                   CategoriaAuditoria categoria,
                                                   String acao,
                                                   Instant dataInicio,
                                                   Instant dataFim,
                                                   String busca) {

        Specification<LogAuditoria> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuarioId"), usuarioId));
            }

            if (categoria != null) {
                predicates.add(cb.equal(root.get("categoria"), categoria));
            }

            if (acao != null && !acao.isBlank()) {
                predicates.add(cb.equal(root.get("acao"), acao.trim()));
            }

            if (dataInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), dataInicio));
            }

            if (dataFim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("criadoEm"), dataFim));
            }

            if (busca != null && !busca.isBlank()) {
                String likePattern = "%" + busca.trim().toLowerCase() + "%";
                Predicate buscaEmail = cb.like(cb.lower(root.get("usuarioEmail")), likePattern);
                Predicate buscaNome = cb.like(cb.lower(root.get("usuarioNome")), likePattern);
                Predicate buscaDetalhes = cb.like(cb.lower(root.get("detalhes")), likePattern);
                Predicate buscaRecurso = cb.like(cb.lower(root.get("recursoId")), likePattern);
                Predicate buscaIp = cb.like(cb.lower(root.get("ip")), likePattern);

                predicates.add(cb.or(buscaEmail, buscaNome, buscaDetalhes, buscaRecurso, buscaIp));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return logAuditoriaRepository.findAll(spec, pageable)
                .map(LogAuditoriaResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public EstatisticasAuditoriaDTO obterEstatisticas() {
        Instant inicioDoDia = LocalDate.now(DataFlexivelUtil.ZONE_BRASIL).atStartOfDay(DataFlexivelUtil.ZONE_BRASIL).toInstant();

        long totalLoginsHoje = logAuditoriaRepository.countByAcaoDesde("LOGIN_SUCESSO", inicioDoDia);
        long totalFalhasLoginHoje = logAuditoriaRepository.countByAcaoDesde("LOGIN_FALHA", inicioDoDia);
        long totalCancelamentosHoje = logAuditoriaRepository.countByAcaoDesde("CANCELAR", inicioDoDia);

        Specification<LogAuditoria> specDesdeHoje = (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("criadoEm"), inicioDoDia);
        long totalAcoesHoje = logAuditoriaRepository.count(specDesdeHoje);

        return new EstatisticasAuditoriaDTO(totalLoginsHoje, totalFalhasLoginHoje, totalAcoesHoje, totalCancelamentosHoje);
    }
}
