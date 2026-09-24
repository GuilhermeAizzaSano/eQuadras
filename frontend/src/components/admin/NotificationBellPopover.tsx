import React, { useState, useRef, useEffect } from 'react';
import { Notificacao } from '../../types';
import { Bell, X, ChevronLeft, ChevronRight } from 'lucide-react';

const defaultFormatador = (dataIso?: string) => {
  if (!dataIso) return '';
  const d = new Date(dataIso);
  if (isNaN(d.getTime())) return dataIso;
  const dia = String(d.getDate()).padStart(2, '0');
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const ano = d.getFullYear();
  const horas = String(d.getHours()).padStart(2, '0');
  const minutos = String(d.getMinutes()).padStart(2, '0');
  return `${dia}/${mes}/${ano} ${horas}:${minutos}`;
};

interface NotificationBellPopoverProps {
  notificacoes: Notificacao[];
  unreadCount: number;
  notificacoesPage: number;
  notificacoesTotalPages: number;
  onCarregarNotificacoes: (page: number) => void;
  onLerNotificacao: (id: number) => void;
  onMarcarTodasComoLidas: () => void;
  onConfirmarExcluirTodas: () => void;
  formatarDataHora?: (iso?: string) => string;
}

export const NotificationBellPopover: React.FC<NotificationBellPopoverProps> = ({
  notificacoes,
  unreadCount,
  notificacoesPage,
  notificacoesTotalPages,
  onCarregarNotificacoes,
  onLerNotificacao,
  onMarcarTodasComoLidas,
  onConfirmarExcluirTodas,
  formatarDataHora = defaultFormatador,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Fecha o popover se clicar fora
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  // Fecha ao pressionar Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2.5 text-fg/60 hover:text-fg transition rounded-xl bg-fg/[0.03] border border-fg/[0.1] hover:border-fg/20 hover:bg-fg/[0.1] cursor-pointer"
        aria-label="Notificações"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-danger rounded-full ring-2 ring-bg" />
        )}
      </button>

      {/* Popover de Notificações */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-surface-2/90 border border-fg/[0.1] rounded-2xl sm:rounded-3xl shadow-apple-elevated z-50 overflow-hidden flex flex-col backdrop-blur-2xl">
          <div className="p-4 border-b border-fg/[0.06] flex justify-between items-center bg-fg/[0.03]">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 rounded-lg bg-fg/[0.06] border border-fg/[0.1] flex items-center justify-center">
                <Bell className="w-3.5 h-3.5 text-fg/80" />
              </div>
              <h3 className="text-xs font-semibold text-fg uppercase tracking-wider font-mono">Notificações</h3>
              {unreadCount > 0 && (
                <span className="text-xs bg-fg/10 text-fg border border-fg/20 font-medium px-1.5 py-0.5 rounded-full font-mono">
                  {unreadCount}
                </span>
              )}
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="p-1.5 rounded-xl text-fg/60 hover:text-fg hover:bg-fg/[0.1] transition cursor-pointer"
              title="Fechar"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Barra de Ações: Marcar Lidas & Excluir Todas */}
          <div className="px-4 py-2 border-b border-fg/[0.06] bg-fg/[0.03] flex items-center justify-between gap-2 min-h-[37px]">
            {notificacoes.length > 0 ? (
              <>
                {notificacoes.some((n) => !n.lida) ? (
                  <button
                    type="button"
                    onClick={onMarcarTodasComoLidas}
                    className="px-2.5 py-1 text-xs font-medium text-info hover:bg-info/10 rounded-lg transition active:scale-95 cursor-pointer"
                  >
                    Marcar tudo lido
                  </button>
                ) : (
                  <span className="text-xs text-fg/60 font-mono px-1">Todas lidas</span>
                )}

                <button
                  type="button"
                  onClick={onConfirmarExcluirTodas}
                  className="px-2.5 py-1 text-xs font-medium text-danger hover:bg-danger/10 rounded-lg transition active:scale-95 cursor-pointer"
                  title="Excluir todas as notificações" aria-label="Excluir todas as notificações"
                >
                  Excluir todas
                </button>
              </>
            ) : (
              <span className="text-xs text-fg/60 font-mono px-1">Nenhuma notificação</span>
            )}
          </div>

          <div className="h-[250px] min-h-[250px] max-h-[250px] overflow-y-auto divide-y divide-fg/[0.06] scrollbar-thin">
            {notificacoes.length === 0 ? (
              <div className="h-full flex items-center justify-center p-8 text-center text-fg/60 text-xs">
                Nenhuma notificação por enquanto.
              </div>
            ) : (
              notificacoes.map((notif) => (
                <div
                  key={notif.id}
                  className={`h-[125px] min-h-[125px] max-h-[125px] p-3.5 flex flex-col justify-between transition ${
                    notif.lida ? 'bg-fg/[0.03] opacity-50' : 'bg-fg/[0.03] hover:bg-fg/[0.06]'
                  }`}
                >
                  <div className="flex-1 overflow-y-auto pr-1 text-xs text-fg/90 leading-relaxed whitespace-pre-line break-words tracking-tight font-sans scrollbar-none">
                    {notif.mensagem}
                  </div>
                  <div className="flex justify-between items-center pt-2 mt-auto shrink-0 border-t border-fg/[0.03]">
                    <span className="text-xs text-fg/60 font-mono">
                      {formatarDataHora(notif.dataCriacao)}
                    </span>
                    {!notif.lida && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onLerNotificacao(notif.id);
                        }}
                        className="text-xs font-medium text-info hover:underline transition active:scale-95 cursor-pointer"
                      >
                        Marcar como lida
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="p-3 border-t border-fg/[0.06] flex items-center justify-between bg-fg/[0.03] text-xs min-h-[45px]">
            <button
              type="button"
              disabled={notificacoesPage === 0}
              onClick={() => onCarregarNotificacoes(notificacoesPage - 1)}
              className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-lg bg-fg/[0.06] hover:bg-fg/[0.1] text-fg/80 disabled:opacity-30 disabled:cursor-not-allowed transition cursor-pointer"
            >
              <ChevronLeft className="w-3.5 h-3.5" />
              <span>Anterior</span>
            </button>
            <span className="text-xs text-fg/60 font-mono">
              Página {notificacoesPage + 1} de {Math.max(1, notificacoesTotalPages)}
            </span>
            <button
              type="button"
              disabled={notificacoesTotalPages <= 1 || notificacoesPage >= notificacoesTotalPages - 1}
              onClick={() => onCarregarNotificacoes(notificacoesPage + 1)}
              className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-lg bg-fg/[0.06] hover:bg-fg/[0.1] text-fg/80 disabled:opacity-30 disabled:cursor-not-allowed transition cursor-pointer"
            >
              <span>Próxima</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
