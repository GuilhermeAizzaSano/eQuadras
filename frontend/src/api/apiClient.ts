import { Usuario, Role, Quadra, HorarioDisponivel, Agendamento, TipoEsporte, DisponibilidadeDia, Page } from '../types';
import type { PageResponse } from '../shared/pagination/types';

export type AbaAgendamento = 'ATIVOS' | 'REALIZADOS' | 'CANCELADOS';

export const getBaseUrl = (): string => {
  const metaEnv = (import.meta as unknown as { env?: { VITE_API_URL?: string } }).env;
  if (metaEnv?.VITE_API_URL) {
    return metaEnv.VITE_API_URL;
  }
  if (typeof window !== 'undefined' && window.location && window.location.hostname) {
    return window.location.origin;
  }
  return window.location.origin;
};

export const getAssetUrl = (url: string): string => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  return `${getBaseUrl()}${url.startsWith('/') ? '' : '/'}${url}`;
};

export const BASE_URL = getBaseUrl();

export interface ApiFetchOptions extends RequestInit {
  timeoutMs?: number;
}

let onUnauthorizedCallback: (() => void) | null = null;

export const setUnauthorizedCallback = (cb: () => void) => {
  onUnauthorizedCallback = cb;
};

export async function apiFetch<T>(
  endpoint: string,
  options: ApiFetchOptions = {}
): Promise<T> {
  const { timeoutMs = 8000, ...fetchOptions } = options;
  const headers = new Headers(fetchOptions.headers || {});

  if (!headers.has('Content-Type') && !(fetchOptions.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  headers.set('X-Client', 'frontend');

  if (!headers.has('X-Correlation-Id')) {
    const correlationId = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `front-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
    headers.set('X-Correlation-Id', correlationId);
  }

  const signal = fetchOptions.signal || (typeof AbortSignal !== 'undefined' && 'timeout' in AbortSignal
    ? AbortSignal.timeout(timeoutMs)
    : undefined);

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...fetchOptions,
    signal,
    credentials: 'include',
    headers,
  });

  const data = await response.json().catch(() => null);

  const extrairMensagemAmigavel = (dados: any, status: number): string => {
    if (dados?.camposIncorretos && Array.isArray(dados.camposIncorretos) && dados.camposIncorretos.length > 0) {
      return dados.camposIncorretos.map((c: any) => `${c.campo || 'Campo'}: ${c.mensagem || 'inválido'}`).join(' | ');
    }
    if (dados?.detail && typeof dados.detail === 'string' && dados.detail.trim().length > 0) {
      return dados.detail;
    }
    if (dados?.mensagem && typeof dados.mensagem === 'string') {
      return dados.mensagem;
    }
    if (dados?.message && typeof dados.message === 'string') {
      const msg = dados.message;
      if (msg.toLowerCase().includes('failed to fetch') || msg.toLowerCase().includes('networkerror')) {
        return 'Não foi possível conectar ao servidor. Verifique sua conexão com a internet.';
      }
      if (msg.toLowerCase().includes('bad credentials')) {
        return 'E-mail ou senha incorretos. Verifique suas credenciais.';
      }
      if (msg.toLowerCase().includes('access is denied')) {
        return 'Você não possui permissão para executar esta ação.';
      }
      return msg;
    }
    if (dados?.title && typeof dados.title === 'string') {
      return dados.title;
    }

    switch (status) {
      case 400:
        return 'Dados da requisição inválidos. Por favor, verifique os campos informados.';
      case 401:
        return 'Sessão expirada ou credenciais inválidas. Faça login novamente.';
      case 403:
        return 'Você não possui permissão para realizar esta operação.';
      case 404:
        return 'O item ou serviço solicitado não foi encontrado.';
      case 409:
        return 'Conflito de dados. O registro ou horário já existe no sistema.';
      case 422:
        return 'Os dados informados não puderam ser processados. Verifique os campos.';
      case 429:
        return 'Muitas tentativas em pouco tempo. Aguarde alguns segundos e tente novamente.';
      case 500:
      case 502:
      case 503:
        return 'O servidor está temporariamente indisponível. Tente novamente em instantes.';
      default:
        return 'Ocorreu um erro ao processar sua solicitação. Tente novamente.';
    }
  };

  if (response.status === 401) {
    const isBootstrapOuLogin = endpoint.includes('/usuarios/me') || endpoint.includes('/usuarios/login');
    if (!isBootstrapOuLogin && onUnauthorizedCallback) {
      onUnauthorizedCallback();
    }
    throw new Error(extrairMensagemAmigavel(data, 401));
  }

  if (response.status === 403) {
    throw new Error(extrairMensagemAmigavel(data, 403));
  }

  if (!response.ok) {
    throw new Error(extrairMensagemAmigavel(data, response.status));
  }

  return data as T;
}

// --- Usuários ---
export const usuarioApi = {
  me: (signal?: AbortSignal) =>
    apiFetch<Usuario>('/usuarios/me', { signal }),

  login: (email_usuario: string, senha_usuario: string, signal?: AbortSignal) =>
    apiFetch<Usuario>('/usuarios/login', {
      method: 'POST',
      body: JSON.stringify({ email_usuario, senha_usuario }),
      signal,
    }),

  logout: (signal?: AbortSignal) =>
    apiFetch<void>('/usuarios/logout', {
      method: 'POST',
      signal,
    }),

  obterApiKeyInfo: (signal?: AbortSignal) =>
    apiFetch<import('../types').ApiKeyInfo>('/usuarios/api-key', { signal }),

  regenerarApiKey: (signal?: AbortSignal) =>
    apiFetch<import('../types').ApiKeyCriada>('/usuarios/api-key/regenerar', {
      method: 'POST',
      signal,
    }),

  revogarApiKey: (signal?: AbortSignal) =>
    apiFetch<void>('/usuarios/api-key', {
      method: 'DELETE',
      signal,
    }),

  cadastrar: (dados: {
    nome_usuario: string;
    email_usuario: string;
    senha_usuario: string;
    phone_usuario: string;
    role?: Role;
  }, signal?: AbortSignal) =>
    apiFetch<Usuario>('/usuarios', {
      method: 'POST',
      body: JSON.stringify(dados),
      signal,
    }),

  editar: (
    id: number,
    dados: {
      nome_usuario: string;
      email_usuario: string;
      phone_usuario: string;
      role?: Role;
      nova_senha?: string;
    },
    signal?: AbortSignal
  ) =>
    apiFetch<Usuario>(`/usuarios/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dados),
      signal,
    }),

  excluir: (id: number, signal?: AbortSignal) =>
    apiFetch<void>(`/usuarios/${id}`, {
      method: 'DELETE',
      signal,
    }),

  alterarMinhaSenha: (dados: { senhaAtual: string; novaSenha: string }, signal?: AbortSignal) =>
    apiFetch<void>('/usuarios/minha-senha', {
      method: 'PATCH',
      body: JSON.stringify(dados),
      signal,
    }),

  listar: (signal?: AbortSignal) => apiFetch<Usuario[]>('/usuarios', { signal }),
};

export interface QuadraFiltroParams {
  latitude?: number;
  longitude?: number;
  raioKm?: number;
  nome?: string;
  endereco?: string;
  tipoEsporte?: string;
  cidade?: string;
  bairro?: string;
  cep?: string;
  page?: number;
  size?: number;
}

// --- Quadras ---
export const quadraApi = {
  listar: (paramsOrLat?: number | QuadraFiltroParams, lon?: number, raioKm?: number) => {
    let url = '/quadras';
    const params = new URLSearchParams();
    if (typeof paramsOrLat === 'object' && paramsOrLat !== null) {
      if (paramsOrLat.latitude !== undefined && paramsOrLat.longitude !== undefined) {
        params.append('latitude', paramsOrLat.latitude.toString());
        params.append('longitude', paramsOrLat.longitude.toString());
        if (paramsOrLat.raioKm !== undefined) {
          params.append('raioKm', paramsOrLat.raioKm.toString());
        }
      }
      if (paramsOrLat.nome) params.append('nome', paramsOrLat.nome);
      if (paramsOrLat.endereco) params.append('endereco', paramsOrLat.endereco);
      if (paramsOrLat.tipoEsporte) params.append('tipoEsporte', paramsOrLat.tipoEsporte);
      if (paramsOrLat.cidade) params.append('cidade', paramsOrLat.cidade);
      if (paramsOrLat.bairro) params.append('bairro', paramsOrLat.bairro);
      if (paramsOrLat.cep) params.append('cep', paramsOrLat.cep);
      if (paramsOrLat.page !== undefined) params.append('page', paramsOrLat.page.toString());
      if (paramsOrLat.size !== undefined) params.append('size', paramsOrLat.size.toString());
    } else if (paramsOrLat !== undefined && lon !== undefined) {
      params.append('latitude', paramsOrLat.toString());
      params.append('longitude', lon.toString());
      if (raioKm !== undefined) {
        params.append('raioKm', raioKm.toString());
      }
    }
    const queryString = params.toString();
    if (queryString) {
      url += `?${queryString}`;
    }
    return apiFetch<Quadra[]>(url);
  },

  listarPaginado: (filtros: QuadraFiltroParams = {}, page = 0, size = 6, signal?: AbortSignal) => {
    let url = '/quadras';
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());

    if (filtros.latitude !== undefined && filtros.longitude !== undefined) {
      params.append('latitude', filtros.latitude.toString());
      params.append('longitude', filtros.longitude.toString());
      if (filtros.raioKm !== undefined) {
        params.append('raioKm', filtros.raioKm.toString());
      }
    }
    if (filtros.nome) params.append('nome', filtros.nome);
    if (filtros.endereco) params.append('endereco', filtros.endereco);
    if (filtros.tipoEsporte) params.append('tipoEsporte', filtros.tipoEsporte);
    if (filtros.cidade) params.append('cidade', filtros.cidade);
    if (filtros.bairro) params.append('bairro', filtros.bairro);
    if (filtros.cep) params.append('cep', filtros.cep);

    const queryString = params.toString();
    if (queryString) {
      url += `?${queryString}`;
    }
    return apiFetch<Page<Quadra>>(url, { signal });
  },

  cadastrar: (dados: { nome: string; tipoEsporte: TipoEsporte; valorHora: number; cep?: string; logradouro?: string; bairro?: string; cidade?: string; estado?: string; latitude?: number; longitude?: number; descricao?: string; dataLimiteAgendamento?: string; fotos?: string[]; disponibilidades?: DisponibilidadeDia[] }) =>
    apiFetch<Quadra>('/quadras', { method: 'POST', body: JSON.stringify(dados) }),

  editar: (id: number, dados: { nome: string; tipoEsporte: TipoEsporte; valorHora: number; cep?: string; logradouro?: string; bairro?: string; cidade?: string; estado?: string; latitude?: number; longitude?: number; descricao?: string; dataLimiteAgendamento?: string; fotos?: string[]; disponibilidades?: DisponibilidadeDia[] }) =>
    apiFetch<Quadra>(`/quadras/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),

  excluir: (id: number) =>
    apiFetch<void>(`/quadras/${id}`, { method: 'DELETE' }),

  alternarStatus: (id: number, ativa: boolean) =>
    apiFetch<Quadra>(`/quadras/${id}/status?ativa=${ativa}`, { method: 'PATCH' }),

  uploadFotos: (id: number, files: File[]) => {
    const formData = new FormData();
    files.forEach((file) => formData.append('fotos', file));
    return apiFetch<Quadra>(`/quadras/${id}/fotos`, { method: 'POST', body: formData, timeoutMs: 30000 });
  },

  removerFoto: (id: number, fotoUrl: string) =>
    apiFetch<Quadra>(`/quadras/${id}/fotos?fotoUrl=${encodeURIComponent(fotoUrl)}`, { method: 'DELETE' }),
};

// --- Bloqueios de Quadra ---
export const bloqueioApi = {
  listar: (quadraId: number) =>
    apiFetch<import('../types').BloqueioHorario[]>(`/quadras/${quadraId}/bloqueios`),

  listarTodosAdmin: () =>
    apiFetch<import('../types').BloqueioHorario[]>('/quadras/bloqueios'),

  criar: (quadraId: number, dados: { data: string; horaInicio?: string; horaFim?: string; motivo?: string; substituirDiaInteiro?: boolean }) =>
    apiFetch<import('../types').BloqueioHorario>(`/quadras/${quadraId}/bloqueios`, {
      method: 'POST',
      body: JSON.stringify(dados),
    }),

  remover: (quadraId: number, bloqueioId: number) =>
    apiFetch<void>(`/quadras/${quadraId}/bloqueios/${bloqueioId}`, { method: 'DELETE' }),

  desbloquear: (quadraId: number, dados: { bloqueioId?: number; data?: string; horaInicio?: string; horaFim?: string }) =>
    apiFetch<{ mensagem: string; totalRemovidos: number }>(`/quadras/${quadraId}/desbloquear`, {
      method: 'POST',
      body: JSON.stringify(dados),
    }),
};

// --- Agendamentos ---
export const agendamentoApi = {
  listar: (historico = false) =>
    apiFetch<Agendamento[]>(historico ? '/agendamentos?historico=true' : '/agendamentos'),

  listarPaginado: (params: { page: number; size: number; aba?: AbaAgendamento; apenasPendentes?: boolean; signal?: AbortSignal }) => {
    const query = new URLSearchParams({
      page: params.page.toString(),
      size: params.size.toString(),
    });
    if (params.aba) {
      query.append('aba', params.aba);
    }
    if (params.apenasPendentes) {
      query.append('apenasPendentes', 'true');
    }
    return apiFetch<PageResponse<Agendamento>>(`/agendamentos?${query.toString()}`, { signal: params.signal });
  },

  obterContadores: (signal?: AbortSignal) =>
    apiFetch<Record<AbaAgendamento, number>>('/agendamentos/contadores', { signal }),

  listarPorQuadra: (quadraId: number) =>
    apiFetch<Agendamento[]>(`/agendamentos/quadra/${quadraId}`),

  buscarPorId: (id: number) => apiFetch<Agendamento>(`/agendamentos/${id}`),

  agendar: (dados: { quadraId: number; dataHoraInicio: string; dataHoraFim: string }) =>
    apiFetch<Agendamento>('/agendamentos', { method: 'POST', body: JSON.stringify(dados) }),

  listarHorariosDisponiveis: (quadraId: number, dataIso: string) =>
    apiFetch<HorarioDisponivel[]>(`/agendamentos/quadra/${quadraId}/horarios-disponiveis?data=${dataIso}`),

  listarHorariosDoDiaAdmin: (dataIso: string) =>
    apiFetch<Record<number, HorarioDisponivel[]>>(`/agendamentos/dia?data=${dataIso}`),

  cancelar: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/agendamentos/${agendamentoId}/cancelar`, { method: 'PATCH' }),
};

// --- Notificações ---
export const notificacaoApi = {
  listarPorAdmin: (page = 0, size = 5) => apiFetch<import('../types').Page<import('../types').Notificacao>>(`/notificacoes/admin?page=${page}&size=${size}`),
  marcarComoLida: (id: number) => apiFetch<void>(`/notificacoes/${id}/ler`, { method: 'PUT' }),
  marcarTodasComoLidas: () => apiFetch<void>('/notificacoes/ler-todas', { method: 'PUT' }),
  excluirTodas: () => apiFetch<void>('/notificacoes/todas', { method: 'DELETE' }),
};

// --- Pagamentos ---
export const pagamentoApi = {
  consultarStatus: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/pagamentos/${agendamentoId}/status`),

  simularAprovacao: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/pagamentos/${agendamentoId}/simular-aprovacao`, { method: 'POST' }),
};

// --- Auditoria & Logs (Exclusivo Master Admin) ---
export interface FiltrosAuditoria {
  page?: number;
  size?: number;
  usuarioId?: number;
  categoria?: import('../types').CategoriaAuditoria;
  acao?: string;
  dataInicio?: string;
  dataFim?: string;
  busca?: string;
}

export const auditoriaApi = {
  listarLogs: (filtros: FiltrosAuditoria = {}, signal?: AbortSignal) => {
    const params = new URLSearchParams();
    if (filtros.page !== undefined) params.append('page', filtros.page.toString());
    if (filtros.size !== undefined) params.append('size', filtros.size.toString());
    if (filtros.usuarioId) params.append('usuarioId', filtros.usuarioId.toString());
    if (filtros.categoria) params.append('categoria', filtros.categoria);
    if (filtros.acao) params.append('acao', filtros.acao);
    if (filtros.dataInicio) params.append('dataInicio', filtros.dataInicio);
    if (filtros.dataFim) params.append('dataFim', filtros.dataFim);
    if (filtros.busca) params.append('busca', filtros.busca);

    const queryString = params.toString() ? `?${params.toString()}` : '';
    return apiFetch<import('../types').Page<import('../types').LogAuditoria>>(`/api/admin/auditoria${queryString}`, { signal });
  },

  obterEstatisticas: (signal?: AbortSignal) =>
    apiFetch<import('../types').EstatisticasAuditoria>('/api/admin/auditoria/estatisticas', { signal }),
};
