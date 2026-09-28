import { describe, it, expect, vi, afterEach } from 'vitest';
import { quadraApi } from './apiClient';

describe('quadraApi.cadastrar', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('envia a Idempotency-Key recebida', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id_quadra: 1 }), { status: 201 }));
    vi.stubGlobal('fetch', fetchMock);

    await quadraApi.cadastrar({ nome: 'Quadra', tipoEsporte: 'FUTSAL', valorHora: 10 }, 'chave-123');

    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get('Idempotency-Key')).toBe('chave-123');
  });
});
