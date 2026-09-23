import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Pagination } from './Pagination';

describe('Pagination', () => {
  it('renderiza "Página 1 de 3" quando page=0 e totalPages=3', () => {
    render(
      <Pagination
        page={0}
        totalPages={3}
        onPageChange={vi.fn()}
      />
    );

    expect(screen.getByText(/página 1 de 3/i)).toBeDefined();
  });

  it('desabilita botão "Anterior" na página 0 e habilita "Próxima"', () => {
    render(
      <Pagination
        page={0}
        totalPages={3}
        onPageChange={vi.fn()}
      />
    );

    const prevBtn = screen.getByRole('button', { name: /página anterior/i }) as HTMLButtonElement;
    const nextBtn = screen.getByRole('button', { name: /próxima página/i }) as HTMLButtonElement;

    expect(prevBtn.disabled).toBe(true);
    expect(nextBtn.disabled).toBe(false);
  });

  it('desabilita botão "Próxima" na última página (page=2, totalPages=3) e habilita "Anterior"', () => {
    render(
      <Pagination
        page={2}
        totalPages={3}
        onPageChange={vi.fn()}
      />
    );

    const prevBtn = screen.getByRole('button', { name: /página anterior/i }) as HTMLButtonElement;
    const nextBtn = screen.getByRole('button', { name: /próxima página/i }) as HTMLButtonElement;

    expect(prevBtn.disabled).toBe(false);
    expect(nextBtn.disabled).toBe(true);
  });

  it('ao clicar em "Próxima", dispara onPageChange com page + 1', () => {
    const handlePageChange = vi.fn();
    render(
      <Pagination
        page={0}
        totalPages={3}
        onPageChange={handlePageChange}
      />
    );

    const nextBtn = screen.getByRole('button', { name: /próxima página/i });
    fireEvent.click(nextBtn);

    expect(handlePageChange).toHaveBeenCalledTimes(1);
    expect(handlePageChange).toHaveBeenCalledWith(1);
  });

  it('ao clicar em "Anterior" (na página 1), dispara onPageChange com page - 1', () => {
    const handlePageChange = vi.fn();
    render(
      <Pagination
        page={1}
        totalPages={3}
        onPageChange={handlePageChange}
      />
    );

    const prevBtn = screen.getByRole('button', { name: /página anterior/i });
    fireEvent.click(prevBtn);

    expect(handlePageChange).toHaveBeenCalledTimes(1);
    expect(handlePageChange).toHaveBeenCalledWith(0);
  });

  it('não renderiza nada quando totalPages <= 1 (retorna null)', () => {
    const { container: c1 } = render(
      <Pagination
        page={0}
        totalPages={1}
        onPageChange={vi.fn()}
      />
    );
    expect(c1.firstChild).toBeNull();

    const { container: c0 } = render(
      <Pagination
        page={0}
        totalPages={0}
        onPageChange={vi.fn()}
      />
    );
    expect(c0.firstChild).toBeNull();
  });

  it('desabilita ambos os botões quando isLoading === true', () => {
    render(
      <Pagination
        page={1}
        totalPages={3}
        isLoading={true}
        onPageChange={vi.fn()}
      />
    );

    const prevBtn = screen.getByRole('button', { name: /página anterior/i }) as HTMLButtonElement;
    const nextBtn = screen.getByRole('button', { name: /próxima página/i }) as HTMLButtonElement;

    expect(prevBtn.disabled).toBe(true);
    expect(nextBtn.disabled).toBe(true);
  });

  it('contém atributos de acessibilidade (aria-label e type="button")', () => {
    render(
      <Pagination
        page={0}
        totalPages={3}
        onPageChange={vi.fn()}
      />
    );

    const prevBtn = screen.getByLabelText('Página anterior');
    const nextBtn = screen.getByLabelText('Próxima página');

    expect(prevBtn).toBeDefined();
    expect(nextBtn).toBeDefined();
    expect(prevBtn.getAttribute('type')).toBe('button');
    expect(nextBtn.getAttribute('type')).toBe('button');
  });

  it('exibe totalElements quando fornecido', () => {
    render(
      <Pagination
        page={0}
        totalPages={3}
        totalElements={55}
        onPageChange={vi.fn()}
      />
    );

    expect(screen.getByText(/55 registros/i)).toBeDefined();
  });
});
