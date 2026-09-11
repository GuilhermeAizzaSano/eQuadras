import { Usuario, Role, Quadra, HorarioDisponivel, Agendamento, TipoEsporte, LoginResponse, DisponibilidadeDia } from '../types';

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

export async function apiFetch<T>(
  endpoint: string,
  options: ApiFetchOptions = {}
): Promise<T> {
  const { timeoutMs = 8000, ...fetchOptions } = options;
  const headers = new Headers(fetchOptions.headers || {});

  if (!headers.has('Content-Type') && !(fetchOptions.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = localStorage.getItem('equadras_auth_token');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
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

  if (!response.ok) {
    const errorMsg = data?.detail || data?.mensagem || data?.message || data?.title || 'Erro na requisição';
    throw new Error(errorMsg);
  }

  return data as T;
}

// --- Usuários ---
export const usuarioApi = {
  login: (email_usuario: string, senha_usuario: string) =>
    apiFetch<LoginResponse>('/usuarios/login', {
      method: 'POST',
      body: JSON.stringify({ email_usuario, senha_usuario }),
    }),

  logout: () =>
    apiFetch<void>('/usuarios/logout', {
      method: 'POST',
    }),

  cadastrar: (dados: {
    nome_usuario: string;
    email_usuario: string;
    senha_usuario: string;
    phone_usuario: string;
    role?: Role;
  }) =>
    apiFetch<Usuario>('/usuarios', {
      method: 'POST',
      body: JSON.stringify(dados),
    }),

  editar: (
    id: number,
    dados: {
      nome_usuario: string;
      email_usuario: string;
      phone_usuario: string;
      role?: Role;
      nova_senha?: string;
    }
  ) =>
    apiFetch<Usuario>(`/usuarios/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dados),
    }),

  excluir: (id: number) =>
    apiFetch<void>(`/usuarios/${id}`, {
      method: 'DELETE',
    }),

  alterarMinhaSenha: (dados: { senhaAtual: string; novaSenha: string }) =>
    apiFetch<void>('/usuarios/minha-senha', {
      method: 'PATCH',
      body: JSON.stringify(dados),
    }),

  listar: () => apiFetch<Usuario[]>('/usuarios'),
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
  listarPorAdmin: () => apiFetch<import('../types').Notificacao[]>('/notificacoes/admin'),
  marcarComoLida: (id: number) => apiFetch<void>(`/notificacoes/${id}/ler`, { method: 'PUT' }),
  marcarTodasComoLidas: () => apiFetch<void>('/notificacoes/ler-todas', { method: 'PUT' }),
};

// --- Pagamentos ---
export const pagamentoApi = {
  consultarStatus: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/pagamentos/${agendamentoId}/status`),

  simularAprovacao: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/pagamentos/${agendamentoId}/simular-aprovacao`, { method: 'POST' }),
};
