import { describe, it, expect } from 'vitest';
import { filtrarProximasPartidas } from './proximasPartidas';
import type { Agendamento } from '../types';

const ag = (id: number, inicio: string, fim: string, status: Agendamento['status'] = 'CONFIRMADO'): Agendamento => ({
  id_agendamento: id, quadraId: 1, nomeQuadra: 'Q1', usuarioId: 1, nomeUsuario: `U${id}`,
  telefoneUsuario: '', dataHoraInicio: inicio, dataHoraFim: fim, valorTotal: 100, status,
  criadoEm: '2026-09-24T08:00:00',
});

describe('filtrarProximasPartidas', () => {
  const agora = new Date(2026, 8, 24, 18, 30, 0);
  const hojeIso = '2026-09-24';

  it('inclui em andamento e as que começam em até 4h, só de hoje, ordenadas e sem canceladas', () => {
    const lista = [
      ag(1, '2026-09-24T21:00:00', '2026-09-24T22:00:00'),              // em breve
      ag(2, '2026-09-24T18:00:00', '2026-09-24T19:00:00'),              // em andamento
      ag(3, '2026-09-24T17:00:00', '2026-09-24T18:00:00'),              // já terminou
      ag(4, '2026-09-24T23:00:00', '2026-09-24T23:59:00'),              // além de 4h
      ag(5, '2026-09-25T00:15:00', '2026-09-25T01:00:00'),              // amanhã
      ag(6, '2026-09-24T19:00:00', '2026-09-24T20:00:00', 'CANCELADO'), // cancelada
    ];

    expect(filtrarProximasPartidas(lista, agora, hojeIso).map((a) => a.id_agendamento)).toEqual([2, 1]);
  });

  it('retorna lista vazia quando não há partidas', () => {
    expect(filtrarProximasPartidas([], agora, hojeIso)).toEqual([]);
  });
});
