package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.dto.request.AgendamentoCriacaoDTO;
import com.agendamentos.equadras.dto.response.AgendamentoResponseDTO;
import com.agendamentos.equadras.dto.response.DashboardMetricasDTO;
import com.agendamentos.equadras.dto.response.HorarioDisponivelDTO;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.AgendaConsultaService;
import com.agendamentos.equadras.service.AgendamentoBotService;
import com.agendamentos.equadras.service.AgendamentoService;
import com.agendamentos.equadras.service.DashboardService;
import com.agendamentos.equadras.service.GradeHorariosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.agendamentos.equadras.model.enums.AbaAgendamento;
import com.agendamentos.equadras.shared.pagination.PageResponse;
import org.springframework.data.domain.Pageable;

@Tag(name = "Agendamentos e Reservas", description = "Endpoints para agendamento concorrente com lock pessimista, verificação de slots e cancelamento de reservas.")
@RestController
@RequestMapping({"/agendamentos", "/api/agendamentos"})
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final DashboardService dashboardService;
    private final AgendamentoBotService agendamentoBotService;
    private final GradeHorariosService gradeHorariosService;
    private final AgendaConsultaService agendaConsultaService;

    public AgendamentoController(AgendamentoService agendamentoService,
                                 DashboardService dashboardService,
                                 AgendamentoBotService agendamentoBotService,
                                 GradeHorariosService gradeHorariosService,
                                 AgendaConsultaService agendaConsultaService) {
        this.agendamentoService = agendamentoService;
        this.dashboardService = dashboardService;
        this.agendamentoBotService = agendamentoBotService;
        this.gradeHorariosService = gradeHorariosService;
        this.agendaConsultaService = agendaConsultaService;
    }

    @Operation(
            summary = "Criar novo agendamento com lock e Pix",
            description = "Papéis: CLIENT ou ADMIN (a reserva pertence ao usuário autenticado; `usuarioId` do corpo é opcional). Bloqueia o horário com lock pessimista, cria a reserva em estado PENDENTE e gera a cobrança Pix (`pixCopiaECola`, `qrCodeBase64`). Regras: horas cheias (minutos zerados), duração mínima de 1 hora e múltipla de 60 min, início no futuro. Efeito colateral: reserva PENDENTE sem pagamento expira e é cancelada automaticamente após 15 minutos. Conflito de horário devolve 409 `HORARIO_INDISPONIVEL`."
    )
    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> agendar(@RequestBody @Valid AgendamentoCriacaoDTO dto,
                                                          @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        AgendamentoResponseDTO resposta = agendamentoService.agendar(dto, usuarioLogado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @Operation(summary = "Listar agendamentos paginados por aba",
            description = "Papéis: CLIENT ou ADMIN. Com `page`: `PageResponse<AgendamentoResponseDTO>`; CLIENT vê as próprias reservas, ADMIN as das suas quadras, Master Admin todas. `aba` é obrigatória, exceto com `apenasPendentes=true` (sem nenhuma das duas, 400). `size` padrão 10, máx. 50. `sort` aceita apenas `dataHoraInicio` e `id`; outro campo devolve 400; a ordenação padrão é crescente por `dataHoraInicio` na aba ATIVOS e decrescente nas demais. SEM `page` (variante legada, `deprecated`, não listada à parte no OpenAPI): lista simples `array<AgendamentoResponseDTO>` limitada a 200 itens no SQL, sem aviso, com o parâmetro `historico` (padrão `false`: só reservas ativas; `true`: histórico completo, inclusive realizadas e canceladas).")
    @GetMapping(params = "page")
    public ResponseEntity<PageResponse<AgendamentoResponseDTO>> listarPaginado(
            @Parameter(description = "Aba: ATIVOS, REALIZADOS ou CANCELADOS. Só vale com `page`; obrigatória quando `apenasPendentes` é `false`.", example = "ATIVOS")
            @RequestParam(required = false) AbaAgendamento aba,
            @Parameter(description = "Só vale com `page`. Se `true`, devolve apenas reservas PENDENTES ainda dentro do prazo de pagamento (15 min) e dispensa `aba`. Padrão `false`.",
                    schema = @Schema(defaultValue = "false"))
            @RequestParam(required = false, defaultValue = "false") boolean apenasPendentes,
            @ParameterObject Pageable pageable,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(agendamentoService.listarPaginado(usuarioLogado.id(), aba, apenasPendentes, pageable));
    }

    @Operation(summary = "Contadores de agendamentos por aba",
            description = "Papéis: CLIENT ou ADMIN. Total das reservas do próprio usuário por aba: ATIVOS, REALIZADOS e CANCELADOS.")
    @GetMapping("/contadores")
    public ResponseEntity<Map<AbaAgendamento, Long>> obterContadores(
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(agendamentoService.contarPorAba(usuarioLogado.id()));
    }

    @Operation(summary = "Listar agendamentos do usuário (por aba)",
            description = """
                Papéis: CLIENT ou ADMIN. Duas variantes, escolhidas pela presença de `page`.

                **Com `page` (recomendada):** `PageResponse<AgendamentoResponseDTO>`; CLIENT vê as próprias reservas, ADMIN as das suas quadras, Master Admin todas. `aba` é obrigatória, exceto com `apenasPendentes=true` (sem nenhuma das duas, 400). `size` padrão 10, máx. 50. `sort` aceita apenas `dataHoraInicio` e `id`; outro campo devolve 400. A ordenação padrão é crescente por `dataHoraInicio` na aba ATIVOS e decrescente nas demais.

                **Sem `page` (legada, prefira sempre `page`):** lista simples `array<AgendamentoResponseDTO>` de até 200 itens, limite aplicado no SQL e sem aviso ao cliente. `historico` define o conteúdo (padrão `false`: só reservas ativas; `true`: histórico completo, inclusive realizadas e canceladas). Nesta variante `aba`, `apenasPendentes`, `size` e `sort` são ignorados.""")
    @GetMapping(params = "!page")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarTodos(
            @Parameter(description = "Só vale SEM `page`. Se `true`, devolve o histórico completo (inclusive realizadas e canceladas). Padrão `false`: só reservas ativas.", schema = @Schema(defaultValue = "false"))
            @RequestParam(required = false, defaultValue = "false") boolean historico,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(agendamentoService.listarTodos(usuarioLogado.id(), historico));
    }

    @Operation(summary = "Buscar agendamento por ID",
            description = "Papéis: CLIENT ou ADMIN. Dono da reserva, administrador da quadra ou Master Admin. `pixCopiaECola` e `qrCodeBase64` só vêm preenchidos para o dono e enquanto o status é PENDENTE. ID inexistente OU fora do seu escopo devolve 404 `AGENDAMENTO_NAO_ENCONTRADO` (nunca 403).")
    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoResponseDTO> buscarPorId(@Parameter(description = "ID do agendamento (`id_agendamento`)", example = "42") @PathVariable Long id,
                                                               @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id, usuarioLogado.id()));
    }

    @Operation(summary = "Cancelar agendamento",
            description = "Papéis: CLIENT ou ADMIN. Dono da reserva ou administrador da quadra. Só cancela reserva futura ainda não cancelada (400 se já cancelada, em andamento ou retroativa). Libera o horário e grava `canceladoEm`.")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(@Parameter(description = "ID do agendamento (`id_agendamento`)", example = "42") @PathVariable Long id,
                                                           @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(agendamentoService.cancelar(id, usuarioLogado.id()));
    }

    @Operation(summary = "Listar reservas de uma quadra (paginado, Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Com `page`: `PageResponse<AgendamentoResponseDTO>` (size padrão 10, máx. 50; `sort` só `dataHoraInicio` ou `id`). SEM `page`: variante legada, lista simples até 200 itens (rota deprecated).")
    @GetMapping(value = "/quadra/{quadraId}", params = "page")
    public ResponseEntity<PageResponse<AgendamentoResponseDTO>> listarPorQuadraPaginado(
            @Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long quadraId,
            @Parameter(description = "Opcional. Aba: ATIVOS, REALIZADOS ou CANCELADOS. Só vale com `page`.", example = "ATIVOS")
            @RequestParam(required = false) AbaAgendamento aba,
            @ParameterObject Pageable pageable,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(agendamentoService.listarPorQuadraPaginado(quadraId, aba, pageable, usuarioLogado.id()));
    }

    @Operation(summary = "Contadores das reservas de uma quadra (Admin)",
            description = "Papel: ADMIN dono da quadra ou Master Admin. Devolve TODOS, ATIVOS, REALIZADOS e CANCELADOS.")
    @GetMapping("/quadra/{quadraId}/contadores")
    public ResponseEntity<Map<String, Long>> obterContadoresPorQuadra(
            @Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long quadraId,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(agendamentoService.contarPorAbaEQuadra(quadraId, usuarioLogado.id()));
    }

    @Operation(summary = "Listar reservas de uma quadra (Admin)",
            description = """
                Papel: ADMIN dono da quadra ou Master Admin. Duas variantes, escolhidas pela presença de `page`.

                **Com `page` (recomendada):** `PageResponse<AgendamentoResponseDTO>`, com `aba` opcional. `size` padrão 10, máx. 50; `sort` aceita apenas `dataHoraInicio` ou `id`.

                **Sem `page` (legada, prefira sempre `page`):** lista simples de até 200 itens, limite aplicado no SQL e sem aviso ao cliente. Nesta variante `aba`, `size` e `sort` são ignorados.""")
    @GetMapping(value = "/quadra/{quadraId}", params = "!page")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarPorQuadra(
            @Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long quadraId,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(agendamentoService.listarPorQuadra(quadraId, usuarioLogado.id()));
    }


    @Operation(
            summary = "Consultar horários de uma quadra na data",
            description = "Papéis: CLIENT ou ADMIN. Grade de horários de 1 hora com `status`: DISPONIVEL, BLOQUEADO, AGENDADO ou INDISPONIVEL, o `motivo` e o booleano `disponivel`. `data` é obrigatória (ISO, `yyyy-MM-dd`); quadra inexistente devolve 400."
    )
    @GetMapping("/quadra/{quadraId}/horarios-disponiveis")
    public ResponseEntity<List<HorarioDisponivelDTO>> listarHorariosDisponiveis(
            @Parameter(description = "ID da quadra (`id_quadra`)", example = "1") @PathVariable Long quadraId,
            @Parameter(description = "Data consultada, formato ISO `yyyy-MM-dd`. Obrigatório.", example = "2026-10-12")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(gradeHorariosService.listarHorariosDisponiveis(quadraId, data));
    }

    @Operation(
            summary = "Consultar horários do dia de todas as quadras do Admin (Admin)",
            description = "Papel: ADMIN. Devolve, em um objeto cuja chave é o ID da quadra, a grade da data para todas as quadras ativas do administrador autenticado."
    )
    @GetMapping("/dia")
    public ResponseEntity<java.util.Map<Long, List<HorarioDisponivelDTO>>> listarHorariosDoDiaParaAdmin(
            @Parameter(description = "Data consultada, formato ISO `yyyy-MM-dd`. Obrigatório.", example = "2026-10-12")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(gradeHorariosService.listarHorariosDoDiaParaAdmin(data, usuarioLogado.id()));
    }

    @Operation(summary = "Listar agenda paginada do dia ou intervalo (Admin)",
            description = "Papel: ADMIN (quadras do administrador autenticado). Informe `data` OU o par `inicio` + `fim`; sem nenhum, 400. Intervalo `[inicio, fim)` de no máximo 24 h. `PageResponse` com size padrão 10, máx. 50; `sort` só `dataHoraInicio` ou `id`. `quadraId` e `aba` refinam o resultado.")
    @GetMapping("/agenda")
    public ResponseEntity<PageResponse<AgendamentoResponseDTO>> listarAgendaDoDia(
            @Parameter(description = "Dia consultado (ISO `yyyy-MM-dd`). Alternativa a `inicio`+`fim`.", example = "2026-10-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Parameter(description = "Início do intervalo (ISO `yyyy-MM-ddTHH:mm:ss`, inclusivo). Exige `fim`.", example = "2026-10-12T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fim do intervalo (ISO `yyyy-MM-ddTHH:mm:ss`, exclusivo). Exige `inicio`. Máximo de 24 h após `inicio`.", example = "2026-10-13T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @Parameter(description = "Restringe a uma quadra.", example = "1")
            @RequestParam(required = false) Long quadraId,
            @Parameter(description = "Opcional. Aba: ATIVOS, REALIZADOS ou CANCELADOS.", example = "ATIVOS")
            @RequestParam(required = false) AbaAgendamento aba,
            @ParameterObject Pageable pageable,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        if (data == null && (inicio == null || fim == null)) {
            throw new IllegalArgumentException("Parâmetro 'data' ou intervalo ('inicio' e 'fim') é obrigatório.");
        }
        return ResponseEntity.ok(agendaConsultaService.listarAgendaDoDiaPaginado(usuarioLogado.id(), data, inicio, fim, quadraId, aba, pageable));
    }

    @Operation(summary = "Contadores da agenda por aba (Admin)",
            description = "Papel: ADMIN. Contagem por aba (ATIVOS, REALIZADOS, CANCELADOS) para `data` ou `inicio`+`fim` e, opcionalmente, uma `quadraId`.")
    @GetMapping("/agenda/contadores")
    public ResponseEntity<Map<AbaAgendamento, Long>> obterContadoresAgendaDoDia(
            @Parameter(description = "Dia consultado (ISO `yyyy-MM-dd`). Alternativa a `inicio`+`fim`.", example = "2026-10-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Parameter(description = "Início do intervalo (ISO `yyyy-MM-ddTHH:mm:ss`, inclusivo). Exige `fim`.", example = "2026-10-12T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fim do intervalo (ISO `yyyy-MM-ddTHH:mm:ss`, exclusivo). Exige `inicio`. Máximo de 24 h após `inicio`.", example = "2026-10-13T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @Parameter(description = "Restringe a uma quadra.", example = "1")
            @RequestParam(required = false) Long quadraId,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        if (data == null && (inicio == null || fim == null)) {
            throw new IllegalArgumentException("Parâmetro 'data' ou intervalo ('inicio' e 'fim') é obrigatório.");
        }
        return ResponseEntity.ok(agendaConsultaService.contarAgendaDoDiaPorAba(usuarioLogado.id(), data, inicio, fim, quadraId));
    }

    @Operation(summary = "Listar agenda completa do dia (Admin)",
            description = "Papel: ADMIN. Todos os agendamentos não cancelados de um único dia (máximo 24 h), sem paginação, para a grade operacional da timeline. Informe `data` OU `inicio`+`fim`.")
    @GetMapping("/agenda/completa")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarAgendaCompleta(
            @Parameter(description = "Dia consultado (ISO `yyyy-MM-dd`). Alternativa a `inicio`+`fim`.", example = "2026-10-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Parameter(description = "Início do intervalo (ISO `yyyy-MM-ddTHH:mm:ss`, inclusivo). Exige `fim`.", example = "2026-10-12T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fim do intervalo (ISO `yyyy-MM-ddTHH:mm:ss`, exclusivo). Exige `inicio`. Máximo de 24 h após `inicio`.", example = "2026-10-13T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @Parameter(description = "Restringe a uma quadra.", example = "1")
            @RequestParam(required = false) Long quadraId,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        if (data == null && (inicio == null || fim == null)) {
            throw new IllegalArgumentException("Parâmetro 'data' ou intervalo ('inicio' e 'fim') é obrigatório.");
        }
        return ResponseEntity.ok(agendaConsultaService.listarAgendaCompleta(usuarioLogado.id(), data, inicio, fim, quadraId));
    }

    @Operation(summary = "Listar agenda mensal (Admin)",
            description = "Papel: ADMIN. Agendamentos não cancelados do mês, para o calendário de ocupação das quadras do administrador. `ano` entre 2000 e 2100; `mes` entre 1 e 12.")
    @GetMapping("/agenda/mensal")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarAgendaMensal(
            @Parameter(description = "Ano (2000 a 2100). Obrigatório.", example = "2026")
            @RequestParam int ano,
            @Parameter(description = "Mês (1 a 12). Obrigatório.", example = "10")
            @RequestParam int mes,
            @Parameter(description = "Restringe a uma quadra.", example = "1")
            @RequestParam(required = false) Long quadraId,
            @UsuarioLogado UsuarioAutenticado usuarioLogado
    ) {
        return ResponseEntity.ok(agendaConsultaService.listarAgendaMensal(usuarioLogado.id(), ano, mes, quadraId));
    }

    @Operation(
            summary = "Obter métricas do dashboard (Admin)",
            description = "Papel: ADMIN. Métricas calculadas no banco para o administrador autenticado: `totalQuadras`, `quadrasAtivas`, `totalReservas` (não canceladas), `faturamentoTotal` (reservas confirmadas) e `reservasHoje`."
    )
    @GetMapping("/dashboard/metricas")
    public ResponseEntity<DashboardMetricasDTO> obterMetricasDashboard(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        if (usuarioLogado == null) {
            throw new org.springframework.security.access.AccessDeniedException("Usuário não autenticado.");
        }
        return ResponseEntity.ok(dashboardService.obterMetricasDashboard(usuarioLogado.id()));
    }

    @Operation(
            summary = "Agendamento simplificado via Bot / WhatsApp",
            description = "Rota PÚBLICA (sem autenticação). Cria a reserva a partir de linguagem flexível. Efeito colateral: cria ou vincula o cliente pelo telefone (`telefoneCliente`, DDD + 8 ou 9 dígitos) e nome. Localiza a quadra por `quadraId`, `nomeQuadra` ou `tipoEsporte`. Aceita `data` como `hoje`, `amanha`, dia da semana, `15/09` ou ISO, e horas como `19`, `19h`, `19h30`, `9:00` ou `19:00` (hora inválida devolve 400). Sem `horaFim`, dura 1 hora. Devolve a reserva PENDENTE com Pix; expira em 15 min se não paga."
    )
    @SecurityRequirements
    @PostMapping("/bot")
    public ResponseEntity<AgendamentoResponseDTO> agendarViaBot(
            @RequestBody @Valid com.agendamentos.equadras.dto.request.AgendamentoBotRequestDTO dto) {
        AgendamentoResponseDTO resposta = agendamentoBotService.agendarViaBot(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @Operation(
            summary = "Consultar grade consolidada de horários (busca flexível)",
            description = "Papéis: CLIENT ou ADMIN. Grade de horários por quadra ativa, com filtros combináveis: `data`, `quadraId`, `nomeQuadra`, `tipoEsporte`. Devolve um item por quadra para UM dia. Com `data` reconhecida, é esse dia. Sem `data` (ou não reconhecida), com `apenasDisponiveis=true` devolve o primeiro dia, a partir de hoje e nos próximos 14, que tenha horário livre (nenhum: lista vazia); com `apenasDisponiveis=false` devolve o dia de hoje. `quadraId` de quadra inexistente ou inativa e filtros sem correspondência devolvem lista vazia, não erro. `apenasDisponiveis=true` mantém só os horários livres e omite as quadras sem nenhum."
    )
    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<List<com.agendamentos.equadras.dto.response.GradeHorariosResponseDTO>> consultarGradeHorarios(
            @Parameter(description = "Data flexível: `hoje`, `amanha`, dia da semana, `15/09` ou ISO `yyyy-MM-dd`. Omitida ou não reconhecida: com `apenasDisponiveis=true` vale o primeiro dia com horário livre nos próximos 14 dias; com `false`, vale hoje.", example = "amanha")
            @RequestParam(required = false) String data,
            @Parameter(description = "Restringe a uma quadra pelo ID.", example = "1")
            @RequestParam(required = false) Long quadraId,
            @Parameter(description = "Filtra pelo tipo de esporte (FUTEBOL, FUTSAL, VOLEI, BEACH_TENNIS, BASQUETE, TENIS).", example = "FUTEBOL")
            @RequestParam(required = false) String tipoEsporte,
            @Parameter(description = "Filtra por parte do nome da quadra.", example = "Arena")
            @RequestParam(required = false) String nomeQuadra,
            @Parameter(description = "Se `true`, devolve só horários livres. Padrão `false`.", schema = @Schema(defaultValue = "false"))
            @RequestParam(required = false, defaultValue = "false") boolean apenasDisponiveis
    ) {
        return ResponseEntity.ok(gradeHorariosService.consultarGradeHorariosFlexivel(data, quadraId, tipoEsporte, nomeQuadra, apenasDisponiveis));
    }
}
