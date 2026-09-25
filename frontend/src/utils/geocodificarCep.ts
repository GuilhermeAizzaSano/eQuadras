export interface Coordenadas {
  lat: number;
  lon: number;
}

export interface EnderecoCep {
  logradouro?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
}

const paraCoordenadas = (lat: unknown, lon: unknown): Coordenadas | null => {
  const la = parseFloat(String(lat));
  const lo = parseFloat(String(lon));
  if (!Number.isFinite(la) || !Number.isFinite(lo) || (la === 0 && lo === 0)) return null;
  return { lat: la, lon: lo };
};

const buscarJson = async (url: string, signal?: AbortSignal, headers?: HeadersInit) => {
  try {
    const res = await fetch(url, { signal, headers });
    return res.ok ? await res.json() : null;
  } catch (err) {
    if ((err as Error)?.name === 'AbortError') throw err;
    return null;
  }
};

const viaBrasilApi = async (cep: string, signal?: AbortSignal) => {
  const data = await buscarJson(`https://brasilapi.com.br/api/cep/v2/${cep}`, signal);
  const c = data?.location?.coordinates;
  return paraCoordenadas(c?.latitude, c?.longitude);
};

const viaAwesomeApi = async (cep: string, signal?: AbortSignal) => {
  const data = await buscarJson(`https://cep.awesomeapi.com.br/json/${cep}`, signal);
  return paraCoordenadas(data?.lat, data?.lng);
};

const viaNominatim = async (query: string, signal?: AbortSignal) => {
  const data = await buscarJson(
    `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`,
    signal,
    { 'User-Agent': 'eQuadras-App/1.0' }
  );
  return Array.isArray(data) && data.length > 0 ? paraCoordenadas(data[0].lat, data[0].lon) : null;
};

/**
 * Coordenada aproximada do CEP. Ordem: BrasilAPI v2 → AwesomeAPI → Nominatim (rua + bairro, depois bairro).
 * Não tenta "rua + cidade" sem bairro nem o centro da cidade: em cidades com ruas numeradas
 * isso devolve pontos longe do CEP e distorce a busca no raio de 2 km.
 */
export async function geocodificarCep(
  cep: string,
  endereco: EnderecoCep,
  signal?: AbortSignal
): Promise<Coordenadas | null> {
  const cepNumerico = cep.replace(/\D/g, '');

  const coords = (await viaBrasilApi(cepNumerico, signal)) ?? (await viaAwesomeApi(cepNumerico, signal));
  if (coords) return coords;

  const { logradouro, bairro, localidade, uf } = endereco;
  if (!bairro || !localidade) return null;
  const sufixo = `${localidade}, ${uf || 'SP'}, Brasil`;

  if (logradouro) {
    const porRua = await viaNominatim(`${logradouro}, ${bairro}, ${sufixo}`, signal);
    if (porRua) return porRua;
  }
  return viaNominatim(`${bairro}, ${sufixo}`, signal);
}
