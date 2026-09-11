import type { components } from './api-schema';

export type Schemas = components['schemas'];

export type Role = NonNullable<Schemas['UsuarioResponseDTO']['role']>;

export type TipoEsporte = NonNullable<Schemas['QuadraResponseDTO']['tipoEsporte']>;

export type StatusAgendamento = NonNullable<Schemas['AgendamentoResponseDTO']['status']>;

export type DiaSemana = NonNullable<Schemas['DisponibilidadeDiaDTO']['diaSemana']>;

export type StatusHorario = NonNullable<Schemas['HorarioDisponivelDTO']['status']>;

// Tipos derivados estritamente dos schemas da API com integridade de modelo
export type Usuario = Omit<Schemas['UsuarioResponseDTO'], 'id_usuario' | 'nome_usuario' | 'email_usuario' | 'phone_usuario' | 'role'> & {
  id_usuario: number;
  nome_usuario: string;
  email_usuario: string;
  phone_usuario: string;
  role: Role;
  criadoEm?: string;
};

export type LoginResponse = {
  token: string;
  usuario: Usuario;
};

export type DisponibilidadeDia = Omit<Schemas['DisponibilidadeDiaDTO'], 'diaSemana' | 'horaInicio' | 'horaFim'> & {
  diaSemana: DiaSemana;
  horaInicio: string; // "06:00" ou "06:00:00"
  horaFim: string;    // "23:00" ou "23:00:00"
};

export type BloqueioHorario = Omit<Schemas['BloqueioHorarioResponseDTO'], 'id' | 'quadraId' | 'data' | 'criadoEm'> & {
  id: number;
  quadraId: number;
  data: string;         // "YYYY-MM-DD"
  horaInicio?: string;  // "HH:mm:ss" ou "HH:mm" — undefined = dia inteiro
  horaFim?: string;     // "HH:mm:ss" ou "HH:mm" — undefined = dia inteiro
  motivo?: string;
  criadoEm: string;
};

export type Quadra = Omit<Schemas['QuadraResponseDTO'], 'id_quadra' | 'nome' | 'tipoEsporte' | 'valorHora' | 'ativa' | 'disponibilidades'> & {
  id_quadra: number;
  nome: string;
  tipoEsporte: TipoEsporte;
  valorHora: number;
  ativa: boolean;
  disponibilidades?: DisponibilidadeDia[];
  bloqueios?: BloqueioHorario[];
};

export type HorarioDisponivel = Omit<Schemas['HorarioDisponivelDTO'], 'inicio' | 'fim' | 'disponivel' | 'motivo'> & {
  inicio: string; // "14:00:00"
  fim: string;    // "15:00:00"
  disponivel: boolean;
  status?: StatusHorario;
  motivo: string;
};

export type Agendamento = Omit<Schemas['AgendamentoResponseDTO'], 'id_agendamento' | 'usuarioId' | 'nomeUsuario' | 'quadraId' | 'nomeQuadra' | 'dataHoraInicio' | 'dataHoraFim' | 'valorTotal' | 'status' | 'criadoEm'> & {
  id_agendamento: number;
  usuarioId: number;
  nomeUsuario: string;
  telefoneUsuario?: string;
  quadraId: number;
  nomeQuadra: string;
  dataHoraInicio: string;
  dataHoraFim: string;
  valorTotal: number;
  status: StatusAgendamento;
  transacaoPagamentoId?: string;
  pixCopiaECola?: string;
  qrCodeBase64?: string;
  criadoEm: string;
};

export interface Notificacao {
  id: number;
  mensagem: string;
  lida: boolean;
  dataCriacao: string;
}

// DTOs de Entrada e Operações derivados do Schema OpenAPI
export type UsuarioCriacaoInput = Schemas['UsuarioCriacaoDTO'];
export type UsuarioEdicaoInput = Schemas['UsuarioEdicaoDTO'];
export type QuadraCriacaoInput = Schemas['QuadraCriacaoDTO'];
export type AgendamentoCriacaoInput = Schemas['AgendamentoCriacaoDTO'];
export type BloqueioHorarioCriacaoInput = Schemas['BloqueioHorarioCriacaoDTO'];
export type DesbloqueioHorarioInput = Schemas['DesbloqueioHorarioDTO'];
