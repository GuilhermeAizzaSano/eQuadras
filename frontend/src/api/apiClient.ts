import { Usuario, Quadra, HorarioDisponivel, Agendamento, TipoEsporte, LoginResponse, DisponibilidadeDia } from '../types';

export const getBaseUrl = (): string => {
  const metaEnv = (import.meta as unknown as { env?: { VITE_API_URL?: string } }).env;
  if (metaEnv?.VITE_API_URL) {
    return metaEnv.VITE_API_URL;
  }
  if (typeof window !== 'undefined' && window.location && window.location.hostname) {
    return `http://${window.location.hostname}:8080`;
  }
  return 'http://localhost:8080';
};

export const getAssetUrl = (url: string): string => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  return `${getBaseUrl()}${url.startsWith('/') ? '' : '/'}${url}`;
};

export const BASE_URL = getBaseUrl();

export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = localStorage.getItem('equadras_auth_token');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
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

  cadastrar: (dados: {
    nome_usuario: string;
    email_usuario: string;
    senha_usuario: string;
    phone_usuario: string;
  }) =>
    apiFetch<LoginResponse>('/usuarios', {
      method: 'POST',
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
    return apiFetch<Quadra>(`/quadras/${id}/fotos`, { method: 'POST', body: formData });
  },

  removerFoto: (id: number, fotoUrl: string) =>
    apiFetch<Quadra>(`/quadras/${id}/fotos?fotoUrl=${encodeURIComponent(fotoUrl)}`, { method: 'DELETE' }),
};

// --- Bloqueios de Quadra ---
export const bloqueioApi = {
  listar: (quadraId: number) =>
    apiFetch<import('../types').BloqueioHorario[]>(`/quadras/${quadraId}/bloqueios`),

  criar: (quadraId: number, dados: { data: string; horaInicio?: string; horaFim?: string; motivo?: string }) =>
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
  listar: () => apiFetch<Agendamento[]>('/agendamentos'),

  agendar: (dados: { quadraId: number; dataHoraInicio: string; dataHoraFim: string }) =>
    apiFetch<Agendamento>('/agendamentos', { method: 'POST', body: JSON.stringify(dados) }),

  listarHorariosDisponiveis: (quadraId: number, dataIso: string) =>
    apiFetch<HorarioDisponivel[]>(`/agendamentos/quadra/${quadraId}/horarios-disponiveis?data=${dataIso}`),

  cancelar: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/agendamentos/${agendamentoId}/cancelar`, { method: 'PATCH' }),
};

// --- Notificações ---
export const notificacaoApi = {
  listarPorAdmin: () => apiFetch<import('../types').Notificacao[]>('/notificacoes/admin'),
  marcarComoLida: (id: number) => apiFetch<void>(`/notificacoes/${id}/ler`, { method: 'PUT' }),
};

// --- Pagamentos ---
export const pagamentoApi = {
  simularAprovacao: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/pagamentos/${agendamentoId}/simular-aprovacao`, { method: 'POST' }),
};
