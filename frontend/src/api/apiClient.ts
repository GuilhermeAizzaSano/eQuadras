import { Usuario, Role, Quadra, HorarioDisponivel, Agendamento, TipoEsporte, DisponibilidadeDia } from '../types';

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

  if (response.status === 401) {
    const isBootstrapOuLogin = endpoint.includes('/usuarios/me') || endpoint.includes('/usuarios/login');
    if (!isBootstrapOuLogin && onUnauthorizedCallback) {
      onUnauthorizedCallback();
    }
    const errorMsg = data?.detail || data?.mensagem || data?.message || data?.title || 'Sessão expirada. Faça login novamente.';
    throw new Error(errorMsg);
  }

  if (response.status === 403) {
    const errorMsg = data?.detail || data?.mensagem || data?.message || data?.title || 'Acesso negado para esta operação.';
    throw new Error(errorMsg);
  }

  if (!response.ok) {
    const errorMsg = data?.detail || data?.mensagem || data?.message || data?.title || 'Erro na requisição';
    throw new Error(errorMsg);
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

// --- Quadras ---
export const quadraApi = {
  listar: (lat?: number, lon?: number, raioKm?: number) => {
    let url = '/quadras';
    const params = new URLSearchParams();
    if (lat !== undefined && lon !== undefined) {
      params.append('latitude', lat.toString());
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
