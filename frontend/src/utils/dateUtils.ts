/**
 * Utilitários para parsing e manipulação de datas no fuso horário local,
 * prevenindo distorções causadas por conversões automáticas para UTC.
 */

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
  const agora = new Date();
  const ano = agora.getFullYear();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
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
