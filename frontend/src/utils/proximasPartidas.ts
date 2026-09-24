import type { Agendamento } from '../types';
import { parseDataHoraLocal } from './dateUtils';

const JANELA_PROXIMAS_PARTIDAS_MS = 4 * 60 * 60 * 1000;

/** Partidas de hoje em andamento ou que começam nas próximas 4 horas, ordenadas pelo início. */
export const filtrarProximasPartidas = (agendamentos: Agendamento[], agora: Date, hojeIso: string): Agendamento[] => {
  const limite = new Date(agora.getTime() + JANELA_PROXIMAS_PARTIDAS_MS);
  return agendamentos
    .filter((ag) => {
      if (ag.status === 'CANCELADO') return false;
      if (ag.dataHoraInicio.split('T')[0] !== hojeIso) return false;
      const inicio = parseDataHoraLocal(ag.dataHoraInicio);
      const fim = parseDataHoraLocal(ag.dataHoraFim);
      const emAndamento = inicio <= agora && fim > agora;
      const emBreve = inicio >= agora && inicio <= limite;
      return emAndamento || emBreve;
    })
    .sort((a, b) => parseDataHoraLocal(a.dataHoraInicio).getTime() - parseDataHoraLocal(b.dataHoraInicio).getTime());
};
