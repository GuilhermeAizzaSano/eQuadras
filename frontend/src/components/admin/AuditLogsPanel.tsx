import React, { useState, useEffect, useCallback } from 'react';
import { auditoriaApi, FiltrosAuditoria } from '../../api/apiClient';
import { LogAuditoria, EstatisticasAuditoria, CategoriaAuditoria } from '../../types';
import { Badge, Button } from '../ui';
import {
  ShieldAlert,
  Search,
  RefreshCw,
  LogIn,
  AlertTriangle,
  Activity,
  CalendarX2,
  ChevronLeft,
  ChevronRight,
  Filter,
  Globe,
} from 'lucide-react';

const CATEGORIAS: { label: string; value: CategoriaAuditoria | 'TODAS' }[] = [
  { label: 'Todas as Categorias', value: 'TODAS' },
  { label: 'Autenticação & Sessão', value: 'AUTENTICACAO' },
  { label: 'Agendamentos', value: 'AGENDAMENTO' },
  { label: 'Quadras', value: 'QUADRA' },
  { label: 'Usuários', value: 'USUARIO' },
  { label: 'Bloqueios', value: 'BLOQUEIO' },
  { label: 'Chaves de API', value: 'API_KEY' },
];

const ACOES: { label: string; value: string }[] = [
  { label: 'Todas as Ações', value: 'TODAS' },
  { label: 'LOGIN_SUCESSO', value: 'LOGIN_SUCESSO' },
  { label: 'LOGIN_FALHA', value: 'LOGIN_FALHA' },
  { label: 'LOGOUT', value: 'LOGOUT' },
  { label: 'ALTERAR_SENHA', value: 'ALTERAR_SENHA' },
  { label: 'CRIAR', value: 'CRIAR' },
  { label: 'EDITAR', value: 'EDITAR' },
  { label: 'EXCLUIR', value: 'EXCLUIR' },
  { label: 'STATUS', value: 'STATUS' },
  { label: 'CANCELAR', value: 'CANCELAR' },
];

export const AuditLogsPanel: React.FC = () => {
  const [logs, setLogs] = useState<LogAuditoria[]>([]);
  const [totalLogs, setTotalLogs] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [erroCarregamento, setErroCarregamento] = useState<string | null>(null);
  const [estatisticas, setEstatisticas] = useState<EstatisticasAuditoria | null>(null);

  // Filtros
  const [busca, setBusca] = useState('');
  const [categoria, setCategoria] = useState<CategoriaAuditoria | 'TODAS'>('TODAS');
  const [acao, setAcao] = useState<string>('TODAS');

  const carregarDados = useCallback(async (paginaAlvo = page) => {
    setLoading(true);
    setErroCarregamento(null);
    try {
      const filtros: FiltrosAuditoria = {
        page: paginaAlvo,
        size: 10,
      };

      if (categoria !== 'TODAS') filtros.categoria = categoria;
      if (acao !== 'TODAS') filtros.acao = acao;
      if (busca.trim()) filtros.busca = busca.trim();

      const resLogs = await auditoriaApi.listarLogs(filtros);

      setLogs(resLogs.content || []);
      setTotalPages(resLogs.totalPages || 0);
      setTotalLogs(resLogs.totalElements || 0);
      setPage(resLogs.number || 0);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Falha ao carregar registros de auditoria';
      console.error('Falha ao carregar trilha de auditoria:', err);
      setErroCarregamento(msg);
    } finally {
      setLoading(false);
    }
  }, [page, categoria, acao, busca]);

  // Estatísticas são do dia e independem de página/filtros: carregar só na abertura e no "Atualizar"
  const carregarEstatisticas = useCallback(async () => {
    try {
      setEstatisticas(await auditoriaApi.obterEstatisticas());
    } catch (err: unknown) {
      console.error('Falha ao carregar estatísticas de auditoria:', err);
    }
  }, []);

  useEffect(() => {
    carregarEstatisticas();
  }, [carregarEstatisticas]);

  useEffect(() => {
    carregarDados(0);
  }, [categoria, acao]);

  const handleBuscar = (e: React.FormEvent) => {
    e.preventDefault();
    carregarDados(0);
  };

  const handleLimparFiltros = () => {
    setBusca('');
    setCategoria('TODAS');
    setAcao('TODAS');
    setPage(0);
  };

  const formatarData = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return iso;
    }
  };

  const renderBadgeAcao = (acaoStr: string) => {
    if (acaoStr === 'LOGIN_SUCESSO') {
      return <Badge variant="success">LOGIN SUCESSO</Badge>;
    }
    if (acaoStr === 'LOGIN_FALHA') {
      return <Badge variant="danger">LOGIN FALHA</Badge>;
    }
    if (acaoStr === 'LOGOUT') {
      return <Badge variant="outline">LOGOUT</Badge>;
    }
    if (acaoStr === 'CANCELAR') {
      return <Badge variant="warning">CANCELADO</Badge>;
    }
    if (acaoStr === 'CRIAR') {
      return <Badge variant="active">CRIADO</Badge>;
    }
    if (acaoStr === 'EDITAR') {
      return <Badge variant="info">EDITADO</Badge>;
    }
    if (acaoStr === 'EXCLUIR') {
      return <Badge variant="danger">EXCLUÍDO</Badge>;
    }
    if (acaoStr === 'STATUS') {
      return <Badge variant="neutral">STATUS</Badge>;
    }
    return <Badge variant="outline">{acaoStr}</Badge>;
  };

  return (
    <div className="space-y-6">
      {/* Header & Estatísticas de Auditoria de Hoje */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {/* Total Logins Hoje */}
        <div className="bg-surface-2 border border-fg/[0.1] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-fg/60 uppercase tracking-wider font-mono">
              Logins Hoje
            </span>
            <div className="w-8 h-8 rounded-xl bg-success/10 flex items-center justify-center text-success">
              <LogIn className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-fg mt-2 font-mono">
            {estatisticas ? estatisticas.totalLoginsHoje : '—'}
          </p>
          <span className="text-xs text-success/80 mt-1 block">Sessões autenticadas</span>
        </div>

        {/* Falhas de Login Hoje */}
        <div className="bg-surface-2 border border-fg/[0.1] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-fg/60 uppercase tracking-wider font-mono">
              Falhas Login
            </span>
            <div className={`w-8 h-8 rounded-xl flex items-center justify-center ${
              estatisticas && estatisticas.totalFalhasLoginHoje > 0
                ? 'bg-danger/20 text-danger animate-pulse'
                : 'bg-fg/[0.06] text-fg/60'
            }`}>
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <p className={`text-2xl font-bold mt-2 font-mono ${
            estatisticas && estatisticas.totalFalhasLoginHoje > 0 ? 'text-danger' : 'text-fg'
          }`}>
            {estatisticas ? estatisticas.totalFalhasLoginHoje : '—'}
          </p>
          <span className="text-xs text-fg/60 mt-1 block">Tentativas recusadas</span>
        </div>

        {/* Cancelamentos Hoje */}
        <div className="bg-surface-2 border border-fg/[0.1] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-fg/60 uppercase tracking-wider font-mono">
              Cancelamentos
            </span>
            <div className="w-8 h-8 rounded-xl bg-warning/10 flex items-center justify-center text-warning">
              <CalendarX2 className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-fg mt-2 font-mono">
            {estatisticas ? estatisticas.totalCancelamentosHoje : '—'}
          </p>
          <span className="text-xs text-warning/80 mt-1 block">Cliente / Admin / Cron</span>
        </div>

        {/* Total Ações Hoje */}
        <div className="bg-surface-2 border border-fg/[0.1] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-fg/60 uppercase tracking-wider font-mono">
              Ações Auditadas
            </span>
            <div className="w-8 h-8 rounded-xl bg-cyan-500/10 flex items-center justify-center text-cyan-400">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-fg mt-2 font-mono">
            {estatisticas ? estatisticas.totalAcoesHoje : '—'}
          </p>
          <span className="text-xs text-cyan-400/80 mt-1 block">Total de registros hoje</span>
        </div>
      </div>

      {/* Barra de Filtros e Busca */}
      <div className="bg-surface-2/80 backdrop-blur-xl p-4 rounded-2xl sm:rounded-3xl border border-fg/[0.1] space-y-3">
        <form onSubmit={handleBuscar} className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2.5 flex-1">
            {/* Input de Busca Textual */}
            <div className="relative flex-1 min-w-[240px]">
              <Search className="w-4 h-4 text-fg/60 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar por e-mail, nome, detalhes, IP ou recurso..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                className="w-full bg-fg/[0.03] border border-fg/[0.1] rounded-xl pl-9 pr-3 py-2 text-xs text-fg placeholder-fg/30 focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition font-sans"
              />
            </div>

            {/* Select Categoria */}
            <select
              value={categoria}
              onChange={(e) => setCategoria(e.target.value as CategoriaAuditoria | 'TODAS')}
              className="bg-surface-3 border border-fg/[0.1] rounded-xl px-3 py-2 text-xs text-fg/80 focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition cursor-pointer"
            >
              {CATEGORIAS.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>

            {/* Select Ação */}
            <select
              value={acao}
              onChange={(e) => setAcao(e.target.value)}
              className="bg-surface-3 border border-fg/[0.1] rounded-xl px-3 py-2 text-xs text-fg/80 focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition cursor-pointer"
            >
              {ACOES.map((a) => (
                <option key={a.value} value={a.value}>
                  {a.label}
                </option>
              ))}
            </select>

            <Button type="submit" variant="secondary" className="text-xs py-2 cursor-pointer">
              <Filter className="w-3.5 h-3.5" />
              <span>Filtrar</span>
            </Button>

            {(busca || categoria !== 'TODAS' || acao !== 'TODAS') && (
              <button
                type="button"
                onClick={handleLimparFiltros}
                className="text-xs text-fg/60 hover:text-fg underline transition px-2"
              >
                Limpar
              </button>
            )}
          </div>

          <div className="flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => {
                carregarDados(page);
                carregarEstatisticas();
              }}
              disabled={loading}
              className="p-2.5 rounded-xl border border-fg/[0.1] bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/60 hover:text-fg transition disabled:opacity-40 cursor-pointer"
              title="Atualizar registros de auditoria"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </form>
      </div>

      {/* Tabela de Logs */}
      <div className="bg-surface-2 border border-fg/[0.1] rounded-2xl sm:rounded-3xl overflow-hidden shadow-[inset_0_1px_0_0_rgba(255,255,255,0.06)]">
        <div className="overflow-x-auto min-h-[560px]">
          <table className="w-full text-left text-xs text-fg/80 table-fixed min-w-[1000px]">
            <colgroup>
              <col className="w-[140px]" />
              <col className="w-[180px]" />
              <col className="w-[140px]" />
              <col className="w-[140px]" />
              <col />
              <col className="w-[150px]" />
            </colgroup>
            <thead className="bg-fg/[0.03] text-fg/60 uppercase tracking-wider font-semibold border-b border-fg/[0.06] text-xs font-mono">
              <tr>
                <th className="px-5 py-3.5">Data / Hora</th>
                <th className="px-5 py-3.5">Usuário / Executor</th>
                <th className="px-5 py-3.5">Categoria / Ação</th>
                <th className="px-5 py-3.5">Recurso</th>
                <th className="px-5 py-3.5">Detalhes</th>
                <th className="px-5 py-3.5">Origem (IP)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-fg/[0.03] font-sans">
              {loading && logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-24 text-center text-fg/60">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-fg/60" />
                    Carregando trilha de auditoria...
                  </td>
                </tr>
              ) : erroCarregamento ? (
                <tr>
                  <td colSpan={6} className="px-5 py-24 text-center text-danger">
                    <AlertTriangle className="w-8 h-8 mx-auto mb-2 text-danger/80" />
                    <p className="font-medium text-sm text-fg">Não foi possível carregar os logs</p>
                    <p className="text-xs text-danger/80 mt-1 max-w-md mx-auto">{erroCarregamento}</p>
                    <button
                      onClick={() => carregarDados(page)}
                      className="mt-4 px-4 py-1.5 text-xs font-medium rounded-xl bg-fg/[0.1] hover:bg-fg/[0.15] text-fg transition cursor-pointer"
                    >
                      Tentar Novamente
                    </button>
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-24 text-center text-fg/60">
                    <ShieldAlert className="w-8 h-8 mx-auto mb-2 text-fg/60" />
                    Nenhum log de auditoria encontrado para os critérios selecionados.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="h-[56px] hover:bg-fg/[0.03] transition">
                    {/* Data / Hora */}
                    <td className="px-5 py-3.5 whitespace-nowrap text-fg/60 font-mono text-xs">
                      {formatarData(log.criadoEm)}
                    </td>

                    {/* Usuário / Executor */}
                    <td className="px-5 py-3.5 truncate">
                      <div className="space-y-1">
                        <div className="flex items-center gap-1.5 truncate">
                          <span className="font-medium text-fg text-xs truncate" title={log.usuarioNome || log.usuarioEmail || 'Sistema'}>
                            {log.usuarioNome || log.usuarioEmail || 'Sistema'}
                          </span>
                        </div>
                        {log.usuarioEmail && log.usuarioNome && (
                          <div className="text-xs text-fg/60 font-mono truncate" title={log.usuarioEmail}>
                            {log.usuarioEmail}
                          </div>
                        )}
                      </div>
                    </td>

                    {/* Categoria / Ação */}
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <div className="space-y-1">
                        <div>{renderBadgeAcao(log.acao)}</div>
                        <div className="text-xs text-fg/60 font-mono uppercase tracking-wider">
                          {log.categoria}
                        </div>
                      </div>
                    </td>

                    {/* Entidade & Recurso */}
                    <td className="px-5 py-3.5 whitespace-nowrap font-mono text-xs">
                      {log.entidade ? (
                        <span className="text-fg/70">
                          {log.entidade}
                          {log.recursoId ? ` #${log.recursoId}` : ''}
                        </span>
                      ) : (
                        <span className="text-fg/60">—</span>
                      )}
                    </td>

                    {/* Detalhes */}
                    <td className="px-5 py-3.5 text-fg/70 text-xs leading-relaxed">
                      <span className="line-clamp-2" title={log.detalhes || ''}>
                        {log.detalhes || '—'}
                      </span>
                    </td>

                    {/* Origem (IP & User-Agent) */}
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <div className="space-y-0.5">
                        <span className="inline-flex items-center gap-1 text-xs font-mono text-fg/60 bg-fg/[0.03] px-2 py-0.5 rounded border border-fg/[0.06]">
                          <Globe className="w-2.5 h-2.5 text-fg/60" />
                          {log.ip || '127.0.0.1'}
                        </span>
                        {log.userAgent && (
                          <div
                            className="text-xs text-fg/60 truncate max-w-[130px]"
                            title={log.userAgent}
                          >
                            {log.userAgent}
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Rodapé com Paginação */}
        <div className="px-5 py-3.5 border-t border-fg/[0.06] flex items-center justify-between text-xs text-fg/60 bg-fg/[0.03]">
          <span>
            Exibindo <strong className="text-fg">{logs.length}</strong> de{' '}
            <strong className="text-fg">{totalLogs}</strong> eventos
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={() => carregarDados(page - 1)}
              disabled={page <= 0 || loading}
              className="p-1.5 rounded-lg border border-fg/[0.1] hover:bg-fg/[0.06] disabled:opacity-30 disabled:cursor-not-allowed transition"
              title="Página anterior"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="text-xs font-mono px-2">
              Página {totalPages > 0 ? page + 1 : 0} de {totalPages}
            </span>
            <button
              onClick={() => carregarDados(page + 1)}
              disabled={page >= totalPages - 1 || loading}
              className="p-1.5 rounded-lg border border-fg/[0.1] hover:bg-fg/[0.06] disabled:opacity-30 disabled:cursor-not-allowed transition"
              title="Próxima página"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
