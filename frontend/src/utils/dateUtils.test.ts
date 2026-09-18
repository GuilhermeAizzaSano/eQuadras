import { describe, it, expect } from 'vitest';
import { parseDataHoraLocal, getHojeLocalIso, formatarDataHoraBr } from './dateUtils';

describe('dateUtils - Proteção contra distorções de fuso e formatação', () => {
  it('deve extrair hoje no formato ISO (YYYY-MM-DD)', () => {
    const hojeIso = getHojeLocalIso();
    expect(hojeIso).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('deve formatar data brasileira corretamente com e sem hora', () => {
    expect(formatarDataHoraBr('2026-09-18')).toBe('18/09/2026');
    expect(formatarDataHoraBr('2026-09-18T14:30:00')).toBe('18/09/2026 às 14:30');
  });

  it('deve fazer parse de data/hora local sem converter para UTC incorretamente', () => {
    const d = parseDataHoraLocal('2026-09-18T15:00:00');
    expect(d.getFullYear()).toBe(2026);
    expect(d.getMonth()).toBe(8); // Setembro (0-indexed)
    expect(d.getDate()).toBe(18);
    expect(d.getHours()).toBe(15);
  });
});
