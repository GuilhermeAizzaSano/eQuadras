import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CourtManagementList } from './CourtManagementList';
import { Quadra } from '../../types';

describe('CourtManagementList', () => {
  const criarQuadra = (id: number, nome: string): Quadra => ({
    id_quadra: id,
    nome,
    tipoEsporte: 'FUTEBOL',
    valorHora: 120,
    descricao: 'Quadra de teste',
    ativa: true,
    dataLimiteAgendamento: '2026-12-31',
    fotos: [],
  });

  const propsBase = {
    mapaBloqueiosPorQuadra: {},
    onAbrirCriacao: vi.fn(),
    onAlternarStatus: vi.fn(),
    onAbrirBloqueios: vi.fn(),
    onAbrirEdicao: vi.fn(),
    onExcluirQuadra: vi.fn(),
    onAbrirHistorico: vi.fn(),
    getAssetUrl: (p: string) => p,
  };

  it('deve exibir EmptyState quando não houver quadras', () => {
    render(<CourtManagementList {...propsBase} minhasQuadras={[]} />);
    expect(screen.getByText('Nenhuma quadra cadastrada')).toBeDefined();
  });

  it('não deve renderizar paginação quando houver 6 quadras ou menos', () => {
    const quadras = Array.from({ length: 4 }, (_, i) => criarQuadra(i + 1, `Quadra ${i + 1}`));
    render(<CourtManagementList {...propsBase} minhasQuadras={quadras} />);

    expect(screen.getByText('Quadra 1')).toBeDefined();
    expect(screen.getByText('Quadra 4')).toBeDefined();
    expect(screen.queryByLabelText('Página anterior')).toBeNull();
    expect(screen.queryByLabelText('Próxima página')).toBeNull();
  });

  it('deve utilizar o componente Pagination compartilhado e paginar entre páginas', () => {
    const quadras = Array.from({ length: 8 }, (_, i) => criarQuadra(i + 1, `Arena ${i + 1}`));
    render(<CourtManagementList {...propsBase} minhasQuadras={quadras} />);

    // Página 1: Arenas 1 a 6
    expect(screen.getByText('Arena 1')).toBeDefined();
    expect(screen.getByText('Arena 6')).toBeDefined();
    expect(screen.queryByText('Arena 7')).toBeNull();
    expect(screen.getByText(/Página 1 de 2/)).toBeDefined();

    // Clicar em Próxima página
    const btnProxima = screen.getByLabelText('Próxima página');
    expect(btnProxima).toBeDefined();
    fireEvent.click(btnProxima);

    // Página 2: Arenas 7 e 8
    expect(screen.getByText('Arena 7')).toBeDefined();
    expect(screen.getByText('Arena 8')).toBeDefined();
    expect(screen.queryByText('Arena 1')).toBeNull();
    expect(screen.getByText(/Página 2 de 2/)).toBeDefined();
  });
});
