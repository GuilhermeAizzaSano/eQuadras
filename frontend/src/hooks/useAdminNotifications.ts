import { useState, useEffect, useRef, useCallback } from 'react';
import { notificacaoApi, getBaseUrl } from '../api/apiClient';
import { Notificacao } from '../types';

interface UseAdminNotificationsOptions {
  user: unknown;
  onNotificationReceived?: (notification: Notificacao) => void;
}

export const useAdminNotifications = ({
  user,
  onNotificationReceived,
}: UseAdminNotificationsOptions) => {
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const [notificacoesPage, setNotificacoesPage] = useState(0);
  const [notificacoesTotalPages, setNotificacoesTotalPages] = useState(0);
  const eventSourceRef = useRef<EventSource | null>(null);

  const carregarNotificacoes = useCallback(async (page = 0) => {
    if (!user) return;
    try {
      const data = await notificacaoApi.listarPorAdmin(page, 5);
      setNotificacoes(data.content);
      setNotificacoesPage(data.number);
      setNotificacoesTotalPages(data.totalPages);
    } catch (err) {
      console.error(err);
    }
  }, [user]);

  const excluirTodasNotificacoes = useCallback(async () => {
    await notificacaoApi.excluirTodas();
    setNotificacoes([]);
    setNotificacoesPage(0);
    setNotificacoesTotalPages(0);
  }, []);

  const lerNotificacao = useCallback(async (id: number) => {
    try {
      await notificacaoApi.marcarComoLida(id);
      setNotificacoes((prev) => prev.map((n) => (n.id === id ? { ...n, lida: true } : n)));
    } catch (err) {
      console.error(err);
    }
  }, []);

  const marcarTodasComoLidas = useCallback(async () => {
    try {
      setNotificacoes((prev) => prev.map((n) => ({ ...n, lida: true })));
      await notificacaoApi.marcarTodasComoLidas();
    } catch (err) {
      console.error('Erro ao marcar todas notificações como lidas:', err);
      carregarNotificacoes();
    }
  }, [carregarNotificacoes]);

  useEffect(() => {
    carregarNotificacoes();

    if (user) {
      const streamUrl = `${getBaseUrl()}/notificacoes/stream`;
      eventSourceRef.current = new EventSource(streamUrl, { withCredentials: true });

      eventSourceRef.current.addEventListener('notificacao', (event) => {
        try {
          const novaNotificacao: Notificacao = JSON.parse(event.data);
          setNotificacoes((prev) => [novaNotificacao, ...prev.slice(0, 4)]);
          onNotificationReceived?.(novaNotificacao);
        } catch {
          // Payload parsing guard
        }
      });

      eventSourceRef.current.onerror = () => {
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }
      };

      return () => {
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }
      };
    }
  }, [user, carregarNotificacoes, onNotificationReceived]);

  const unreadCount = notificacoes.filter((n) => !n.lida).length;

  return {
    notificacoes,
    notificacoesPage,
    notificacoesTotalPages,
    unreadCount,
    carregarNotificacoes,
    lerNotificacao,
    marcarTodasComoLidas,
    excluirTodasNotificacoes,
  };
};
