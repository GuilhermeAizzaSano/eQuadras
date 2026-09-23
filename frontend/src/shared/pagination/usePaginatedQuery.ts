import { useCallback, useEffect, useRef, useState } from 'react';
import type { PageResponse } from './types';

export interface PageFetchParams<F> {
  page: number;
  size: number;
  filters: F;
  signal: AbortSignal;
}

export type PageFetcher<T, F> = (params: PageFetchParams<F>) => Promise<PageResponse<T>>;

export type PaginatedStatus = 'loading' | 'success' | 'error';

interface Options {
  size: number;
}

/**
 * Paginação server-side genérica.
 * - `filters` deve ser serializável em JSON (é usado como chave de identidade).
 * - Trocar filtros volta para a página 0.
 * - Respostas obsoletas são descartadas (AbortController + checagem de abort).
 * - Dados anteriores permanecem visíveis durante o carregamento e em caso de erro.
 */
export function usePaginatedQuery<T, F>(fetcher: PageFetcher<T, F>, filters: F, { size }: Options) {
  const filtersKey = JSON.stringify(filters);

  // Página vinculada à chave de filtros: trocar filtro zera a página no mesmo render (sem fetch intermediário).
  const [pageState, setPageState] = useState({ key: filtersKey, page: 0 });
  const page = pageState.key === filtersKey ? pageState.page : 0;

  const [data, setData] = useState<PageResponse<T> | null>(null);
  const [status, setStatus] = useState<PaginatedStatus>('loading');
  const [error, setError] = useState<unknown>(null);
  const [reloadToken, setReloadToken] = useState(0);

  const fetcherRef = useRef(fetcher);
  const filtersRef = useRef(filters);
  useEffect(() => {
    fetcherRef.current = fetcher;
    filtersRef.current = filters;
  });

  const setPage = useCallback(
    (next: number) => setPageState({ key: filtersKey, page: Math.max(0, next) }),
    [filtersKey],
  );

  const reload = useCallback(() => setReloadToken((t) => t + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    setStatus('loading');

    fetcherRef.current({ page, size, filters: filtersRef.current, signal: controller.signal })
      .then((response) => {
        if (controller.signal.aborted) return;

        if (response.totalPages > 0 && page >= response.totalPages) {
          setPageState({ key: filtersKey, page: response.totalPages - 1 });
          return;
        }

        setData(response);
        setError(null);
        setStatus('success');
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        setError(err);
        setStatus('error');
      });

    return () => controller.abort();
  }, [page, size, filtersKey, reloadToken]);

  return {
    items: data?.content ?? [],
    page,
    totalPages: data?.totalPages ?? 0,
    totalElements: data?.totalElements ?? 0,
    status,
    error,
    isEmpty: status === 'success' && (data?.totalElements ?? 0) === 0,
    setPage,
    reload,
  };
}
