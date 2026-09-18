import React, { useState, useEffect, useCallback } from 'react';
import { auditoriaApi, FiltrosAuditoria } from '../../api/apiClient';
import { LogAuditoria, EstatisticasAuditoria, CategoriaAuditoria, TipoExecutor } from '../../types';
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
  User,
  ShieldCheck,
  Server
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

      const [resLogs, resStats] = await Promise.all([
        auditoriaApi.listarLogs(filtros),
        auditoriaApi.obterEstatisticas().catch(() => null),
      ]);

      setLogs(resLogs.content || []);
      setTotalPages(resLogs.totalPages || 0);
      setTotalLogs(resLogs.totalElements || 0);
      setPage(resLogs.number || 0);

      if (resStats) {
        setEstatisticas(resStats);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Falha ao carregar registros de auditoria';
      console.error('Falha ao carregar trilha de auditoria:', err);
      setErroCarregamento(msg);
    } finally {
      setLoading(false);
    }
  }, [page, categoria, acao, busca]);

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

  const renderBadgeExecutor = (tipo: TipoExecutor) => {
    switch (tipo) {
      case 'MASTER_ADMIN':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <ShieldAlert className="w-2.5 h-2.5" />
            Master Admin
          </span>
        );
      case 'ADMIN_QUADRA':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">
            <ShieldCheck className="w-2.5 h-2.5" />
            Admin Quadra
          </span>
        );
      case 'CLIENTE':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <User className="w-2.5 h-2.5" />
            Cliente
          </span>
        );
      case 'SISTEMA':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-500/10 text-purple-400 border border-purple-500/20">
            <Server className="w-2.5 h-2.5" />
            Sistema / Cron
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Estatísticas de Auditoria de Hoje */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {/* Total Logins Hoje */}
        <div className="bg-[#121214] border border-white/[0.08] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-white/50 uppercase tracking-wider font-mono">
              Logins Hoje
            </span>
            <div className="w-8 h-8 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-400">
              <LogIn className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-white mt-2 font-mono">
            {estatisticas ? estatisticas.totalLoginsHoje : '—'}
          </p>
          <span className="text-[11px] text-emerald-400/80 mt-1 block">Sessões autenticadas</span>
        </div>

        {/* Falhas de Login Hoje */}
        <div className="bg-[#121214] border border-white/[0.08] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-white/50 uppercase tracking-wider font-mono">
              Falhas Login
            </span>
            <div className={`w-8 h-8 rounded-xl flex items-center justify-center ${
              estatisticas && estatisticas.totalFalhasLoginHoje > 0
                ? 'bg-rose-500/20 text-rose-400 animate-pulse'
                : 'bg-white/[0.05] text-white/40'
            }`}>
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <p className={`text-2xl font-bold mt-2 font-mono ${
            estatisticas && estatisticas.totalFalhasLoginHoje > 0 ? 'text-rose-400' : 'text-white'
          }`}>
            {estatisticas ? estatisticas.totalFalhasLoginHoje : '—'}
          </p>
          <span className="text-[11px] text-white/40 mt-1 block">Tentativas recusadas</span>
        </div>

        {/* Cancelamentos Hoje */}
        <div className="bg-[#121214] border border-white/[0.08] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-white/50 uppercase tracking-wider font-mono">
              Cancelamentos
            </span>
            <div className="w-8 h-8 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-400">
              <CalendarX2 className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-white mt-2 font-mono">
            {estatisticas ? estatisticas.totalCancelamentosHoje : '—'}
          </p>
          <span className="text-[11px] text-amber-400/80 mt-1 block">Cliente / Admin / Cron</span>
        </div>

        {/* Total Ações Hoje */}
        <div className="bg-[#121214] border border-white/[0.08] p-4 rounded-2xl relative overflow-hidden shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-white/50 uppercase tracking-wider font-mono">
              Ações Auditadas
            </span>
            <div className="w-8 h-8 rounded-xl bg-cyan-500/10 flex items-center justify-center text-cyan-400">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-white mt-2 font-mono">
            {estatisticas ? estatisticas.totalAcoesHoje : '—'}
          </p>
          <span className="text-[11px] text-cyan-400/80 mt-1 block">Total de registros hoje</span>
        </div>
      </div>

      {/* Barra de Filtros e Busca */}
      <div className="bg-[#121214]/80 backdrop-blur-xl p-4 rounded-2xl sm:rounded-3xl border border-white/[0.08] space-y-3">
        <form onSubmit={handleBuscar} className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2.5 flex-1">
            {/* Input de Busca Textual */}
            <div className="relative flex-1 min-w-[240px]">
              <Search className="w-4 h-4 text-white/40 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar por e-mail, nome, detalhes, IP ou recurso..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-white/30 focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition font-sans"
              />
            </div>

            {/* Select Categoria */}
            <select
              value={categoria}
              onChange={(e) => setCategoria(e.target.value as CategoriaAuditoria | 'TODAS')}
              className="bg-[#1c1c1e] border border-white/[0.08] rounded-xl px-3 py-2 text-xs text-white/80 focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition cursor-pointer"
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
              className="bg-[#1c1c1e] border border-white/[0.08] rounded-xl px-3 py-2 text-xs text-white/80 focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition cursor-pointer"
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
                className="text-xs text-white/50 hover:text-white underline transition px-2"
              >
                Limpar
              </button>
            )}
          </div>

          <div className="flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => carregarDados(page)}
              disabled={loading}
              className="p-2.5 rounded-xl border border-white/[0.08] bg-white/[0.04] hover:bg-white/[0.08] text-white/60 hover:text-white transition disabled:opacity-40 cursor-pointer"
              title="Atualizar registros de auditoria"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </form>
      </div>

      {/* Tabela de Logs */}
      <div className="bg-[#121214] border border-white/[0.08] rounded-2xl sm:rounded-3xl overflow-hidden shadow-[inset_0_1px_0_0_rgba(255,255,255,0.06)]">
        <div className="overflow-x-auto min-h-[560px]">
          <table className="w-full text-left text-xs text-white/80 table-fixed min-w-[1000px]">
            <colgroup>
              <col className="w-[140px]" />
              <col className="w-[180px]" />
              <col className="w-[140px]" />
              <col className="w-[140px]" />
              <col />
              <col className="w-[150px]" />
            </colgroup>
            <thead className="bg-white/[0.02] text-white/40 uppercase tracking-wider font-semibold border-b border-white/[0.06] text-[10px] font-mono">
              <tr>
                <th className="px-5 py-3.5">Data / Hora</th>
                <th className="px-5 py-3.5">Usuário / Executor</th>
                <th className="px-5 py-3.5">Categoria / Ação</th>
                <th className="px-5 py-3.5">Recurso</th>
                <th className="px-5 py-3.5">Detalhes</th>
                <th className="px-5 py-3.5">Origem (IP)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.04] font-sans">
              {loading && logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-24 text-center text-white/40">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-white/20" />
                    Carregando trilha de auditoria...
                  </td>
                </tr>
              ) : erroCarregamento ? (
                <tr>
                  <td colSpan={6} className="px-5 py-24 text-center text-rose-400">
                    <AlertTriangle className="w-8 h-8 mx-auto mb-2 text-rose-400/80" />
                    <p className="font-medium text-sm text-white">Não foi possível carregar os logs</p>
                    <p className="text-xs text-rose-400/80 mt-1 max-w-md mx-auto">{erroCarregamento}</p>
                    <button
                      onClick={() => carregarDados(page)}
                      className="mt-4 px-4 py-1.5 text-xs font-medium rounded-xl bg-white/[0.08] hover:bg-white/[0.12] text-white transition cursor-pointer"
                    >
                      Tentar Novamente
                    </button>
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-24 text-center text-white/40">
                    <ShieldAlert className="w-8 h-8 mx-auto mb-2 text-white/20" />
                    Nenhum log de auditoria encontrado para os critérios selecionados.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="h-[56px] hover:bg-white/[0.02] transition">
                    {/* Data / Hora */}
                    <td className="px-5 py-3.5 whitespace-nowrap text-white/50 font-mono text-[11px]">
                      {formatarData(log.criadoEm)}
                    </td>

                    {/* Usuário / Executor */}
                    <td className="px-5 py-3.5 truncate">
                      <div className="space-y-1">
                        <div className="flex items-center gap-1.5 truncate">
                          {renderBadgeExecutor(log.tipoExecutor)}
                          <span className="font-medium text-white text-xs truncate" title={log.usuarioNome || log.usuarioEmail || 'Sistema'}>
                            {log.usuarioNome || log.usuarioEmail || 'Sistema'}
                          </span>
                        </div>
                        {log.usuarioEmail && log.usuarioNome && (
                          <div className="text-[11px] text-white/40 font-mono truncate" title={log.usuarioEmail}>
                            {log.usuarioEmail}
                          </div>
                        )}
                      </div>
                    </td>

                    {/* Categoria / Ação */}
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <div className="space-y-1">
                        <div>{renderBadgeAcao(log.acao)}</div>
                        <div className="text-[10px] text-white/40 font-mono uppercase tracking-wider">
                          {log.categoria}
                        </div>
                      </div>
                    </td>

                    {/* Entidade & Recurso */}
                    <td className="px-5 py-3.5 whitespace-nowrap font-mono text-[11px]">
                      {log.entidade ? (
                        <span className="text-white/70">
                          {log.entidade}
                          {log.recursoId ? ` #${log.recursoId}` : ''}
                        </span>
                      ) : (
                        <span className="text-white/30">—</span>
                      )}
                    </td>

                    {/* Detalhes */}
                    <td className="px-5 py-3.5 text-white/70 text-xs leading-relaxed">
                      <span className="line-clamp-2" title={log.detalhes || ''}>
                        {log.detalhes || '—'}
                      </span>
                    </td>

                    {/* Origem (IP & User-Agent) */}
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <div className="space-y-0.5">
                        <span className="inline-flex items-center gap-1 text-[11px] font-mono text-white/60 bg-white/[0.04] px-2 py-0.5 rounded border border-white/[0.06]">
                          <Globe className="w-2.5 h-2.5 text-white/40" />
                          {log.ip || '127.0.0.1'}
                        </span>
                        {log.userAgent && (
                          <div
                            className="text-[10px] text-white/30 truncate max-w-[130px]"
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
        <div className="px-5 py-3.5 border-t border-white/[0.06] flex items-center justify-between text-xs text-white/50 bg-white/[0.01]">
          <span>
            Exibindo <strong className="text-white">{logs.length}</strong> de{' '}
            <strong className="text-white">{totalLogs}</strong> eventos
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={() => carregarDados(page - 1)}
              disabled={page <= 0 || loading}
              className="p-1.5 rounded-lg border border-white/[0.08] hover:bg-white/[0.06] disabled:opacity-30 disabled:cursor-not-allowed transition"
              title="Página anterior"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="text-[11px] font-mono px-2">
              Página {totalPages > 0 ? page + 1 : 0} de {totalPages}
            </span>
            <button
              onClick={() => carregarDados(page + 1)}
              disabled={page >= totalPages - 1 || loading}
              className="p-1.5 rounded-lg border border-white/[0.08] hover:bg-white/[0.06] disabled:opacity-30 disabled:cursor-not-allowed transition"
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
