import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { PageResponse } from './types';
import { usePaginatedQuery } from './usePaginatedQuery';

const page = <T,>(content: T[], p: number, totalElements: number, size = 5): PageResponse<T> => ({
  content, page: p, size, totalElements, totalPages: Math.ceil(totalElements / size),
});

describe('usePaginatedQuery', () => {
  it('busca a página 0 ao montar e expõe metadados', async () => {
    const fetcher = vi.fn().mockResolvedValue(page([1, 2], 0, 2));
    const { result } = renderHook(() => usePaginatedQuery(fetcher, { aba: 'ATIVOS' }, { size: 5 }));

    await waitFor(() => expect(result.current.status).toBe('success'));
    expect(result.current.items).toEqual([1, 2]);
    expect(result.current.totalPages).toBe(1);
    expect(fetcher).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 5, filters: { aba: 'ATIVOS' } }));
  });

  it('volta para a página 0 ao trocar o filtro, sem buscar a página antiga com o filtro novo', async () => {
    const fetcher = vi.fn().mockImplementation(({ page: p }) => Promise.resolve(page([p], p, 20)));
    const { result, rerender } = renderHook(({ f }) => usePaginatedQuery(fetcher, f, { size: 5 }), {
      initialProps: { f: { aba: 'ATIVOS' } },
    });
    await waitFor(() => expect(result.current.status).toBe('success'));
    act(() => result.current.setPage(2));
    await waitFor(() => expect(result.current.page).toBe(2));

    fetcher.mockClear();
    rerender({ f: { aba: 'CANCELADOS' } });

    await waitFor(() => expect(result.current.page).toBe(0));
    expect(fetcher).not.toHaveBeenCalledWith(expect.objectContaining({ page: 2, filters: { aba: 'CANCELADOS' } }));
  });

  it('ignora resposta obsoleta quando a página muda antes da resposta anterior chegar', async () => {
    const resolvers: Record<number, (v: PageResponse<number>) => void> = {};
    const fetcher = vi.fn(({ page: p }: { page: number }) =>
      new Promise<PageResponse<number>>((resolve) => { resolvers[p] = resolve; }));
    const { result } = renderHook(() => usePaginatedQuery(fetcher, {}, { size: 5 }));

    act(() => result.current.setPage(1));
    await act(async () => resolvers[1](page([6], 1, 10)));
    await act(async () => resolvers[0](page([1], 0, 10)));

    expect(result.current.items).toEqual([6]);
  });

  it('ajusta para a última página válida quando a página atual deixa de existir', async () => {
    const fetcher = vi.fn()
      .mockResolvedValueOnce(page([1], 0, 6))
      .mockResolvedValueOnce(page([], 1, 5))   // página 1 ficou vazia (ex.: após cancelamento)
      .mockResolvedValueOnce(page([1, 2, 3, 4, 5], 0, 5));
    const { result } = renderHook(() => usePaginatedQuery(fetcher, {}, { size: 5 }));
    await waitFor(() => expect(result.current.status).toBe('success'));

    act(() => result.current.setPage(1));

    await waitFor(() => expect(result.current.page).toBe(0));
    expect(result.current.items).toHaveLength(5);
  });

  it('expõe erro sem perder os dados anteriores', async () => {
    const fetcher = vi.fn()
      .mockResolvedValueOnce(page([1], 0, 10))
      .mockRejectedValueOnce(new Error('falhou'));
    const { result } = renderHook(() => usePaginatedQuery(fetcher, {}, { size: 5 }));
    await waitFor(() => expect(result.current.status).toBe('success'));

    act(() => result.current.setPage(1));

    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.items).toEqual([1]);
  });

  it('reload busca novamente a página atual', async () => {
    const fetcher = vi.fn().mockResolvedValue(page([1], 0, 1));
    const { result } = renderHook(() => usePaginatedQuery(fetcher, {}, { size: 5 }));
    await waitFor(() => expect(result.current.status).toBe('success'));

    act(() => result.current.reload());

    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  });
});
