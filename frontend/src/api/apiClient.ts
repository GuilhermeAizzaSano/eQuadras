import { Usuario, Quadra, HorarioDisponivel, Agendamento, TipoEsporte, Role } from '../types';

const BASE_URL = 'http://localhost:8080';

export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {},
  currentUserId?: number
): Promise<T> {
  const headers = new Headers(options.headers || {});
  
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (currentUserId) {
    headers.set('X-Usuario-Id', currentUserId.toString());
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
    apiFetch<Usuario>('/usuarios/login', {
      method: 'POST',
      body: JSON.stringify({ email_usuario, senha_usuario }),
    }),

  cadastrar: (dados: {
    nome_usuario: string;
    email_usuario: string;
    senha_usuario: string;
    phone_usuario: string;
    role: Role;
  }) =>
    apiFetch<Usuario>('/usuarios', {
      method: 'POST',
      body: JSON.stringify(dados),
    }),

  listar: () => apiFetch<Usuario[]>('/usuarios'),
};

// --- Quadras ---
export const quadraApi = {
  listar: (userId?: number, lat?: number, lon?: number, raioKm?: number) => {
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
    return apiFetch<Quadra[]>(url, {}, userId);
  },

  cadastrar: (
    dados: { nome: string; tipoEsporte: TipoEsporte; valorHora: number; cep?: string; logradouro?: string; bairro?: string; cidade?: string; estado?: string; latitude?: number; longitude?: number; descricao?: string; fotos?: string[] },
    adminId: number
  ) =>
    apiFetch<Quadra>(
      '/quadras',
      {
        method: 'POST',
        body: JSON.stringify(dados),
      },
      adminId
    ),

  editar: (
    id: number,
    dados: { nome: string; tipoEsporte: TipoEsporte; valorHora: number; cep?: string; logradouro?: string; bairro?: string; cidade?: string; estado?: string; latitude?: number; longitude?: number; descricao?: string; fotos?: string[] },
    adminId: number
  ) =>
    apiFetch<Quadra>(
      `/quadras/${id}`,
      {
        method: 'PUT',
        body: JSON.stringify(dados),
      },
      adminId
    ),

  excluir: (id: number, adminId: number) =>
    apiFetch<void>(
      `/quadras/${id}`,
      {
        method: 'DELETE',
      },
      adminId
    ),

  alternarStatus: (id: number, ativa: boolean, adminId: number) =>
    apiFetch<Quadra>(
      `/quadras/${id}/status?ativa=${ativa}`,
      { method: 'PATCH' },
      adminId
    ),

  uploadFotos: (id: number, files: File[], adminId: number) => {
    const formData = new FormData();
    files.forEach((file) => formData.append('fotos', file));
    return apiFetch<Quadra>(
      `/quadras/${id}/fotos`,
      {
        method: 'POST',
        body: formData,
      },
      adminId
    );
  },

  removerFoto: (id: number, fotoUrl: string, adminId: number) =>
    apiFetch<Quadra>(
      `/quadras/${id}/fotos?fotoUrl=${encodeURIComponent(fotoUrl)}`,
      {
        method: 'DELETE',
      },
      adminId
    ),
};


// --- Agendamentos ---
export const agendamentoApi = {
  listar: (userId?: number) =>
    apiFetch<Agendamento[]>('/agendamentos', {}, userId),

  agendar: (dados: {
    usuarioId: number;
    quadraId: number;
    dataHoraInicio: string;
    dataHoraFim: string;
  }) =>
    apiFetch<Agendamento>('/agendamentos', {
      method: 'POST',
      body: JSON.stringify(dados),
    }),

  listarHorariosDisponiveis: (quadraId: number, dataIso: string) =>
    apiFetch<HorarioDisponivel[]>(
      `/agendamentos/quadra/${quadraId}/horarios-disponiveis?data=${dataIso}`
    ),

  cancelar: (agendamentoId: number, userId: number) =>
    apiFetch<Agendamento>(
      `/agendamentos/${agendamentoId}/cancelar`,
      { method: 'PATCH' },
      userId
    ),
};

// --- Notificações ---
export const notificacaoApi = {
  listarPorAdmin: (adminId: number) =>
    apiFetch<import('../types').Notificacao[]>(`/notificacoes/admin/${adminId}`),

  marcarComoLida: (id: number) =>
    apiFetch<void>(`/notificacoes/${id}/ler`, {
      method: 'PUT',
    }),
};

// --- Pagamentos ---
export const pagamentoApi = {
  simularAprovacao: (agendamentoId: number) =>
    apiFetch<Agendamento>(`/pagamentos/${agendamentoId}/simular-aprovacao`, {
      method: 'POST',
    }),
};
