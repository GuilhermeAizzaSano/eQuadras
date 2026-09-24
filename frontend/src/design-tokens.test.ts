import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

// Guarda contra regressão do design system: cores devem vir dos tokens de tema
// (ver docs/design-system.md), não de hex fixo nem de paletas do Tailwind.

const RAIZ = path.resolve(__dirname);

function listarTsx(dir: string): string[] {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) return listarTsx(p);
    return /\.tsx$/.test(e.name) && !/\.test\.tsx$/.test(e.name) ? [p] : [];
  });
}

const arquivos = listarTsx(RAIZ);

function violacoes(regex: RegExp): string[] {
  const achados: string[] = [];
  for (const arq of arquivos) {
    const linhas = fs.readFileSync(arq, 'utf8').split(/\r?\n/);
    linhas.forEach((linha, i) => {
      if (regex.test(linha)) achados.push(`${path.relative(RAIZ, arq)}:${i + 1}`);
    });
  }
  return achados;
}

describe('design tokens', () => {
  it('não usa cor hexadecimal arbitrária em classes', () => {
    expect(violacoes(/(?:text|bg|border|ring|from|via|to|shadow|fill|stroke)-\[#[0-9A-Fa-f]{3,8}\]/)).toEqual([]);
  });

  it('não usa paletas de estado do Tailwind (use success/danger/warning/info/purple)', () => {
    expect(violacoes(/(?:text|bg|border|ring|from|via|to|shadow)-(?:emerald|rose|amber|blue|red|green)-\d{2,3}/)).toEqual([]);
  });

  it('não usa bg/border/ring/divide branco ou preto fixos (use fg/bg; scrims usam bg-black/NN)', () => {
    expect(violacoes(/(?<![\w-])(?:[a-z0-9-]+:)*(?:bg|border|ring|divide)-(?:white|black)(?![\w/-])/)).toEqual([]);
  });

  it('não usa texto menor que 12px', () => {
    expect(violacoes(/text-\[(?:9|10|11)px\]/)).toEqual([]);
  });

  it('não usa texto informativo com opacidade abaixo de 60% (placeholder/disabled podem)', () => {
    expect(violacoes(/(?<![\w:-])text-fg\/(?:10|20|25|30|35|40|45|50)(?![\d])/)).toEqual([]);
  });
});
