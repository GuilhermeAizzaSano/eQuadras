import type { FC } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface PaginationProps {
  page: number; // 0-based
  totalPages: number;
  onPageChange: (nextPage: number) => void; // 0-based
  isLoading?: boolean;
  totalElements?: number;
  className?: string;
}

export const Pagination: FC<PaginationProps> = ({
  page,
  totalPages,
  onPageChange,
  isLoading = false,
  totalElements,
  className = '',
}) => {
  if (totalPages <= 1) {
    return null;
  }

  const isFirstPage = page <= 0;
  const isLastPage = page >= totalPages - 1;

  return (
    <nav
      role="navigation"
      aria-label="Paginação"
      className={`flex flex-col sm:flex-row items-center ${
        totalElements !== undefined ? 'justify-between' : 'justify-center'
      } gap-3 py-3 text-xs sm:text-sm text-white/70 ${className}`.trim()}
    >
      {totalElements !== undefined && (
        <span className="text-white/50 select-none">
          {totalElements} registros
        </span>
      )}

      <div className="flex items-center gap-2">
        <button
          type="button"
          aria-label="Página anterior"
          disabled={isFirstPage || isLoading}
          onClick={() => onPageChange(page - 1)}
          className="inline-flex items-center justify-center p-2 rounded-xl border border-white/[0.08] bg-white/[0.04] text-white/80 hover:bg-white/[0.08] hover:text-white transition disabled:opacity-30 disabled:pointer-events-none cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-white/30 active:scale-95"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <span className="font-medium text-white/80 px-2 select-none">
          Página {page + 1} de {totalPages}
        </span>

        <button
          type="button"
          aria-label="Próxima página"
          disabled={isLastPage || isLoading}
          onClick={() => onPageChange(page + 1)}
          className="inline-flex items-center justify-center p-2 rounded-xl border border-white/[0.08] bg-white/[0.04] text-white/80 hover:bg-white/[0.08] hover:text-white transition disabled:opacity-30 disabled:pointer-events-none cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-white/30 active:scale-95"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </nav>
  );
};
