/**
 * Utilitários para parsing e manipulação de datas no fuso horário local,
 * prevenindo distorções causadas por conversões automáticas para UTC.
 */

export interface HorarioBrasilia {
  agora: Date;
  ano: number;
  mes: string;
  dia: string;
  hora: number;
  minuto: number;
  segundo: number;
  hojeIso: string;
  horaMinutoAtual: number;
}

export const getAgoraBrasilia = (): HorarioBrasilia => {
  const agoraUtc = new Date();
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });

  const parts = formatter.formatToParts(agoraUtc);
  const getPart = (type: string) => parts.find((p) => p.type === type)?.value || '';

  const ano = parseInt(getPart('year'), 10) || agoraUtc.getFullYear();
  const mes = getPart('month') || String(agoraUtc.getMonth() + 1).padStart(2, '0');
  const dia = getPart('day') || String(agoraUtc.getDate()).padStart(2, '0');
  let hora = parseInt(getPart('hour'), 10);
  if (isNaN(hora)) hora = agoraUtc.getHours();
  if (hora === 24) hora = 0;
  const minuto = parseInt(getPart('minute'), 10) || 0;
  const segundo = parseInt(getPart('second'), 10) || 0;

  const hojeIso = `${ano}-${mes}-${dia}`;
  const horaMinutoAtual = hora + minuto / 60;
  const agora = new Date(ano, parseInt(mes, 10) - 1, parseInt(dia, 10), hora, minuto, segundo);

  return {
    agora,
    ano,
    mes,
    dia,
    hora,
    minuto,
    segundo,
    hojeIso,
    horaMinutoAtual,
  };
};

export const parseDataHoraLocal = (dataHoraStr: string): Date => {
  if (!dataHoraStr) return new Date();

  const cleanStr = dataHoraStr.replace(' ', 'T');
  const [dataPart, horaPart] = cleanStr.split('T');
  if (!dataPart) return new Date(dataHoraStr);

  const [ano, mes, dia] = dataPart.split('-').map(Number);
  if (!horaPart) {
    return new Date(ano, mes - 1, dia);
  }

  const [hora, min, sec] = horaPart.split(':').map((v) => parseInt(v, 10) || 0);
  return new Date(ano, mes - 1, dia, hora, min, sec);
};

export const getHojeLocalIso = (): string => {
  return getAgoraBrasilia().hojeIso;
};

export const extrairDataIso = (dataHoraStr: string): string => {
  if (!dataHoraStr) return '';
  return dataHoraStr.includes('T') ? dataHoraStr.split('T')[0] : dataHoraStr.slice(0, 10);
};

export const extrairHoraMinuto = (dataHoraStr: string): string => {
  if (!dataHoraStr) return '';
  const parteHora = dataHoraStr.includes('T') ? dataHoraStr.split('T')[1] : dataHoraStr;
  return parteHora.slice(0, 5);
};
