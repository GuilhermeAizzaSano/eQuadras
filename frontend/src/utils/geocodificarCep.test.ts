import { describe, it, expect, vi, afterEach } from 'vitest';
import { geocodificarCep } from './geocodificarCep';

const endereco = { logradouro: 'Rua 7', bairro: 'Jardim Micena', localidade: 'Jales', uf: 'SP' };

const resposta = (body: unknown, ok = true) => Promise.resolve({ ok, json: () => Promise.resolve(body) } as Response);

const mockFetch = (rotas: (url: string) => Promise<Response>) => {
  const fn = vi.fn((url: string) => rotas(url));
  vi.stubGlobal('fetch', fn);
  return fn;
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('geocodificarCep', () => {
  it('usa a coordenada da BrasilAPI sem consultar outras fontes', async () => {
    const fetchMock = mockFetch(() =>
      resposta({ location: { coordinates: { latitude: '-20.26889', longitude: '-50.54583' } } })
    );
    expect(await geocodificarCep('15703-000', endereco)).toEqual({ lat: -20.26889, lon: -50.54583 });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toContain('brasilapi.com.br/api/cep/v2/15703000');
  });

  it('cai para a AwesomeAPI quando a BrasilAPI não traz coordenadas', async () => {
    mockFetch((url) =>
      url.includes('brasilapi')
        ? resposta({ location: { coordinates: {} } })
        : resposta({ lat: '-20.27186', lng: '-50.54873' })
    );
    expect(await geocodificarCep('15703000', endereco)).toEqual({ lat: -20.27186, lon: -50.54873 });
  });

  it('usa o Nominatim com rua + bairro e nunca rua + cidade sem bairro', async () => {
    const fetchMock = mockFetch((url) => {
      if (url.includes('brasilapi')) return resposta({}, false);
      if (url.includes('awesomeapi')) return Promise.reject(new TypeError('Failed to fetch'));
      if (decodeURIComponent(url).includes('Rua 7, Jardim Micena')) return resposta([]);
      return resposta([{ lat: '-20.27', lon: '-50.55' }]);
    });
    expect(await geocodificarCep('15703000', endereco)).toEqual({ lat: -20.27, lon: -50.55 });
    const consultas = fetchMock.mock.calls.map((c) => decodeURIComponent(c[0]));
    expect(consultas.some((u) => u.includes('Rua 7, Jardim Micena, Jales'))).toBe(true);
    expect(consultas.some((u) => u.includes('q=Jardim Micena, Jales'))).toBe(true);
    expect(consultas.some((u) => u.includes('Rua 7, Jales'))).toBe(false);
  });

  it('retorna null quando nenhuma fonte encontra o CEP', async () => {
    mockFetch((url) => (url.includes('nominatim') ? resposta([]) : resposta({}, false)));
    expect(await geocodificarCep('15703000', endereco)).toBeNull();
  });

  it('propaga o cancelamento da requisição', async () => {
    mockFetch(() => Promise.reject(new DOMException('Aborted', 'AbortError')));
    await expect(geocodificarCep('15703000', endereco, new AbortController().signal)).rejects.toMatchObject({
      name: 'AbortError',
    });
  });
});
