export type Role = 'CLIENT' | 'ADMIN';

export type TipoEsporte = 'FUTEBOL' | 'FUTSAL' | 'VOLEI' | 'BEACH_TENNIS' | 'BASQUETE' | 'TENIS';

export type StatusAgendamento = 'PENDENTE' | 'CONFIRMADO' | 'CANCELADO';

export interface Usuario {
  id_usuario: number;
  nome_usuario: string;
  email_usuario: string;
  phone_usuario: string;
  role: Role;
  criadoEm?: string;
}

export interface Quadra {
  id_quadra: number;
  nome: string;
  tipoEsporte: TipoEsporte;
  valorHora: number;
  ativa: boolean;
  cep?: string;
  logradouro?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  latitude?: number;
  longitude?: number;
  descricao?: string;
  fotos?: string[];
}

export interface HorarioDisponivel {
  inicio: string; // "14:00:00"
  fim: string;    // "15:00:00"
  disponivel: boolean;
  motivo: string;
}

export interface Agendamento {
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
}
export interface Notificacao {
  id: number;
  mensagem: string;
  lida: boolean;
  dataCriacao: string;
}
