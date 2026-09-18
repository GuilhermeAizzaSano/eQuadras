import { useState, useCallback } from 'react';
import { Quadra, BloqueioHorario } from '../types';
import { bloqueioApi } from '../api/apiClient';
import { getHojeLocalIso } from '../utils/dateUtils';

interface UseCourtBlocksOptions {
  minhasQuadras: Quadra[];
  onSuccessAction?: () => Promise<void> | void;
  setFeedback: (feedback: { type: 'success' | 'error'; message: string } | null) => void;
  setConfirmModal: (modal: {
    isOpen: boolean;
    title: string;
    description: string;
    isDestructive: boolean;
    confirmLabel?: string;
    cancelLabel?: string;
    onConfirm: () => void;
  }) => void;
}

export const useCourtBlocks = ({
  minhasQuadras,
  onSuccessAction,
  setFeedback,
  setConfirmModal,
}: UseCourtBlocksOptions) => {
  const [bloqueioModalQuadra, setBloqueioModalQuadra] = useState<Quadra | null>(null);
  const [bloqueiosQuadra, setBloqueiosQuadra] = useState<BloqueioHorario[]>([]);
  const [loadingBloqueios, setLoadingBloqueios] = useState(false);
  const [mapaBloqueiosPorQuadra, setMapaBloqueiosPorQuadra] = useState<Record<number, BloqueioHorario[]>>({});
  const [bloqueioData, setBloqueioData] = useState('');
  const [bloqueioHoraInicio, setBloqueioHoraInicio] = useState('');
  const [bloqueioHoraFim, setBloqueioHoraFim] = useState('');
  const [bloqueioMotivo, setBloqueioMotivo] = useState('');
  const [submittingBloqueio, setSubmittingBloqueio] = useState(false);

  const carregarMapaBloqueios = useCallback(async () => {
    try {
      const todosBloqueios = await bloqueioApi.listarTodosAdmin();
      const hojeIso = getHojeLocalIso();
      const novoMapa: Record<number, BloqueioHorario[]> = {};
      todosBloqueios
        .filter((b) => !b.data || b.data >= hojeIso)
        .forEach((b) => {
          if (b.quadraId) {
            if (!novoMapa[b.quadraId]) {
              novoMapa[b.quadraId] = [];
            }
            novoMapa[b.quadraId].push(b);
          }
        });
      setMapaBloqueiosPorQuadra(novoMapa);
    } catch (bErr) {
      console.error('Erro ao carregar mapa de bloqueios consolidado:', bErr);
      setMapaBloqueiosPorQuadra({});
    }
  }, []);

  const carregarBloqueios = useCallback(async (quadraId: number) => {
    setLoadingBloqueios(true);
    try {
      const data = await bloqueioApi.listar(quadraId);
      const hojeIso = getHojeLocalIso();
      setBloqueiosQuadra(data.filter((b) => !b.data || b.data >= hojeIso));
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Erro ao carregar bloqueios.' });
    } finally {
      setLoadingBloqueios(false);
    }
  }, [setFeedback]);

  const abrirGerenciamentoBloqueios = useCallback(async (q: Quadra) => {
    setBloqueioModalQuadra(q);
    setBloqueioData(new Date().toISOString().split('T')[0]);
    setBloqueioHoraInicio('');
    setBloqueioHoraFim('');
    setBloqueioMotivo('');
    await carregarBloqueios(q.id_quadra);
  }, [carregarBloqueios]);

  const handleBloquearSlot = useCallback((quadraId: number, data: string, horaInicio: string, horaFim: string) => {
    const quadra = minhasQuadras.find((q) => q.id_quadra === quadraId);
    if (!quadra) return;
    setBloqueioModalQuadra(quadra);
    setBloqueioData(data);
    setBloqueioHoraInicio(horaInicio);
    setBloqueioHoraFim(horaFim);
    setBloqueioMotivo('');
    carregarBloqueios(quadraId);
  }, [minhasQuadras, carregarBloqueios]);

  const handleDesbloquearSlot = useCallback((
    quadraId: number,
    data: string,
    horaInicio: string,
    horaFim: string,
    bloqueio: BloqueioHorario
  ) => {
    const quadra = minhasQuadras.find((q) => q.id_quadra === quadraId);
    const quadraNome = quadra ? quadra.nome : 'Quadra';
    const dataFormatada = data.split('-').reverse().join('/');

    setConfirmModal({
      isOpen: true,
      title: 'Desbloquear Horário',
      description: `Deseja realmente desbloquear o horário das ${horaInicio} às ${horaFim} na quadra "${quadraNome}" em ${dataFormatada}? O restante dos horários permanecerá bloqueado.`,
      isDestructive: true,
      confirmLabel: 'Sim, desbloquear este horário',
      onConfirm: async () => {
        setConfirmModal({
          isOpen: false,
          title: '',
          description: '',
          isDestructive: false,
          onConfirm: () => {},
        });
        try {
          await bloqueioApi.desbloquear(quadraId, {
            bloqueioId: bloqueio.id,
            data,
            horaInicio: `${horaInicio}:00`,
            horaFim: `${horaFim}:00`,
          });
          setFeedback({
            type: 'success',
            message: `Horário das ${horaInicio} às ${horaFim} desbloqueado com sucesso!`,
          });
          await onSuccessAction?.();
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Erro ao desbloquear horário.' });
        }
      },
    });
  }, [minhasQuadras, setConfirmModal, setFeedback, onSuccessAction]);

  const executarCriacaoBloqueio = useCallback(async (substituirDiaInteiro: boolean = false) => {
    if (!bloqueioModalQuadra || submittingBloqueio) return;

    setSubmittingBloqueio(true);
    try {
      await bloqueioApi.criar(bloqueioModalQuadra.id_quadra, {
        data: bloqueioData,
        horaInicio: bloqueioHoraInicio ? `${bloqueioHoraInicio}:00` : undefined,
        horaFim: bloqueioHoraFim ? `${bloqueioHoraFim}:00` : undefined,
        motivo: bloqueioMotivo || undefined,
        substituirDiaInteiro,
      });

      setFeedback({
        type: 'success',
        message: substituirDiaInteiro
          ? 'Bloqueio do dia todo substituído pelo horário específico com sucesso!'
          : 'Bloqueio adicionado com sucesso!',
      });
      setBloqueioHoraInicio('');
      setBloqueioHoraFim('');
      setBloqueioMotivo('');
      await carregarBloqueios(bloqueioModalQuadra.id_quadra);
      await onSuccessAction?.();
    } catch (err: any) {
      if (err.message && err.message.includes('DIA_INTEIRO_BLOQUEADO')) {
        setConfirmModal({
          isOpen: true,
          title: 'Substituir Bloqueio do Dia Todo',
          description: `A quadra "${bloqueioModalQuadra.nome}" já está bloqueada o dia todo em ${bloqueioData.split('-').reverse().join('/')}. Deseja desbloquear o restante do dia e manter bloqueado apenas o horário das ${bloqueioHoraInicio} às ${bloqueioHoraFim}?`,
          isDestructive: false,
          confirmLabel: 'Sim, desbloquear dia e bloquear horário',
          onConfirm: async () => {
            setConfirmModal({
              isOpen: false,
              title: '',
              description: '',
              isDestructive: false,
              onConfirm: () => {},
            });
            await executarCriacaoBloqueio(true);
          },
        });
        return;
      }
      setFeedback({ type: 'error', message: err.message || 'Erro ao adicionar bloqueio.' });
    } finally {
      setSubmittingBloqueio(false);
    }
  }, [
    bloqueioModalQuadra,
    submittingBloqueio,
    bloqueioData,
    bloqueioHoraInicio,
    bloqueioHoraFim,
    bloqueioMotivo,
    setFeedback,
    carregarBloqueios,
    onSuccessAction,
    setConfirmModal,
  ]);

  const handleCriarBloqueio = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bloqueioModalQuadra) return;

    if (bloqueioHoraInicio && !bloqueioHoraFim) {
      setFeedback({ type: 'error', message: 'Informe também o horário de término do bloqueio.' });
      return;
    }
    if (!bloqueioHoraInicio && bloqueioHoraFim) {
      setFeedback({ type: 'error', message: 'Informe também o horário de início do bloqueio.' });
      return;
    }

    const bloqueiosDaData = bloqueiosQuadra.filter((b) => b.data === bloqueioData);
    const temBloqueioDiaInteiro = bloqueiosDaData.some((b) => !b.horaInicio || !b.horaFim);

    if (bloqueioHoraInicio && bloqueioHoraFim && temBloqueioDiaInteiro) {
      setConfirmModal({
        isOpen: true,
        title: 'Substituir Bloqueio do Dia Todo',
        description: `A quadra "${bloqueioModalQuadra.nome}" já está bloqueada o dia todo em ${bloqueioData.split('-').reverse().join('/')}. Deseja desbloquear o restante do dia e manter bloqueado apenas o horário das ${bloqueioHoraInicio} às ${bloqueioHoraFim}?`,
        isDestructive: false,
        confirmLabel: 'Sim, desbloquear dia e bloquear horário',
        onConfirm: async () => {
          setConfirmModal({
            isOpen: false,
            title: '',
            description: '',
            isDestructive: false,
            onConfirm: () => {},
          });
          await executarCriacaoBloqueio(true);
        },
      });
      return;
    }

    await executarCriacaoBloqueio(false);
  }, [
    bloqueioModalQuadra,
    bloqueioHoraInicio,
    bloqueioHoraFim,
    bloqueiosQuadra,
    bloqueioData,
    setFeedback,
    setConfirmModal,
    executarCriacaoBloqueio,
  ]);

  const handleRemoverBloqueio = useCallback(async (bloqueioId: number) => {
    if (!bloqueioModalQuadra) return;
    try {
      await bloqueioApi.remover(bloqueioModalQuadra.id_quadra, bloqueioId);
      setFeedback({ type: 'success', message: 'Bloqueio removido com sucesso!' });
      await carregarBloqueios(bloqueioModalQuadra.id_quadra);
      await onSuccessAction?.();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Erro ao remover bloqueio.' });
    }
  }, [bloqueioModalQuadra, setFeedback, carregarBloqueios, onSuccessAction]);

  return {
    bloqueioModalQuadra,
    setBloqueioModalQuadra,
    bloqueiosQuadra,
    loadingBloqueios,
    mapaBloqueiosPorQuadra,
    bloqueioData,
    setBloqueioData,
    bloqueioHoraInicio,
    setBloqueioHoraInicio,
    bloqueioHoraFim,
    setBloqueioHoraFim,
    bloqueioMotivo,
    setBloqueioMotivo,
    submittingBloqueio,
    carregarMapaBloqueios,
    carregarBloqueios,
    abrirGerenciamentoBloqueios,
    handleBloquearSlot,
    handleDesbloquearSlot,
    handleCriarBloqueio,
    handleRemoverBloqueio,
  };
};
