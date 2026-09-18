import { useState, useRef, useEffect, useCallback } from 'react';
import { Quadra, TipoEsporte, DiaSemana, DisponibilidadeDia } from '../types';
import { quadraApi } from '../api/apiClient';
import { HorariosPorDia, DEFAULT_HORARIOS, DIAS_SEMANA } from '../components/admin';

interface UseCourtFormOptions {
  user: unknown;
  onSuccessAction?: () => Promise<void> | void;
  setFeedback: (feedback: { type: 'success' | 'error'; message: string } | null) => void;
  setLoading: (loading: boolean) => void;
  setLoadingMessage: (msg: string) => void;
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

export const useCourtForm = ({
  user,
  onSuccessAction,
  setFeedback,
  setLoading,
  setLoadingMessage,
  setConfirmModal,
}: UseCourtFormOptions) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [nome, setNome] = useState('');
  const [tipoEsporte, setTipoEsporte] = useState<TipoEsporte>('FUTEBOL');
  const [valorHora, setValorHora] = useState('');
  const [descricao, setDescricao] = useState('');
  const [dataLimiteAgendamento, setDataLimiteAgendamento] = useState('');
  const [cep, setCep] = useState('');
  const [logradouro, setLogradouro] = useState('');
  const [numero, setNumero] = useState('');
  const [bairro, setBairro] = useState('');
  const [cidade, setCidade] = useState('');
  const [estado, setEstado] = useState('');
  const [latitude, setLatitude] = useState<number | undefined>();
  const [longitude, setLongitude] = useState<number | undefined>();
  const [loadingCep, setLoadingCep] = useState(false);

  const [fotosExistentes, setFotosExistentes] = useState<string[]>([]);
  const [novasFotos, setNovasFotos] = useState<File[]>([]);
  const [novasFotosPreviews, setNovasFotosPreviews] = useState<string[]>([]);
  const [horarios, setHorarios] = useState<HorariosPorDia>(DEFAULT_HORARIOS);

  const previewsRef = useRef<string[]>([]);
  previewsRef.current = novasFotosPreviews;

  useEffect(() => {
    return () => {
      previewsRef.current.forEach((url) => URL.revokeObjectURL(url));
    };
  }, []);

  const fecharModal = useCallback(() => {
    novasFotosPreviews.forEach((url) => URL.revokeObjectURL(url));
    setIsModalOpen(false);
    setEditandoId(null);
    setNome('');
    setValorHora('');
    setDescricao('');
    setDataLimiteAgendamento('');
    setFotosExistentes([]);
    setNovasFotos([]);
    setNovasFotosPreviews([]);
    setCep('');
    setLogradouro('');
    setNumero('');
    setBairro('');
    setCidade('');
    setEstado('');
    setLatitude(undefined);
    setLongitude(undefined);
    setHorarios(DEFAULT_HORARIOS);
  }, [novasFotosPreviews]);

  const abrirModalCriacao = useCallback(() => {
    fecharModal();
    setHorarios(DEFAULT_HORARIOS);
    setIsModalOpen(true);
  }, [fecharModal]);

  const abrirModalEdicao = useCallback((q: Quadra) => {
    novasFotosPreviews.forEach((url) => URL.revokeObjectURL(url));
    setEditandoId(q.id_quadra);
    setNome(q.nome);
    setTipoEsporte(q.tipoEsporte);
    setValorHora(q.valorHora.toString());
    setDescricao(q.descricao || '');
    setDataLimiteAgendamento(q.dataLimiteAgendamento || '');
    setFotosExistentes(q.fotos || []);
    setNovasFotos([]);
    setNovasFotosPreviews([]);
    setCep(q.cep || '');
    setLogradouro(q.logradouro || '');
    setNumero('');
    setBairro(q.bairro || '');
    setCidade(q.cidade || '');
    setEstado(q.estado || '');
    setLatitude(q.latitude);
    setLongitude(q.longitude);

    if (q.disponibilidades && q.disponibilidades.length > 0) {
      const novosHorarios: HorariosPorDia = {
        MONDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        TUESDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        WEDNESDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        THURSDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        FRIDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        SATURDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        SUNDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
      };
      q.disponibilidades.forEach((d) => {
        if (novosHorarios[d.diaSemana]) {
          novosHorarios[d.diaSemana] = {
            ativo: true,
            horaInicio: d.horaInicio ? d.horaInicio.slice(0, 5) : '06:00',
            horaFim: d.horaFim ? d.horaFim.slice(0, 5) : '23:00',
          };
        }
      });
      setHorarios(novosHorarios);
    } else {
      setHorarios(DEFAULT_HORARIOS);
    }

    setIsModalOpen(true);
  }, [novasFotosPreviews]);

  const handleCepChange = useCallback(async (valOrEvent: string | React.ChangeEvent<HTMLInputElement>) => {
    const rawInput = typeof valOrEvent === 'string' ? valOrEvent : valOrEvent.target.value;
    const rawVal = rawInput.replace(/\D/g, '');
    const formatted = rawVal.replace(/^(\d{5})(\d)/, '$1-$2').slice(0, 9);
    setCep(formatted);

    if (rawVal.length === 8) {
      setLoadingCep(true);
      try {
        const res = await fetch(`https://viacep.com.br/ws/${rawVal}/json/`);
        const data = await res.json();
        if (!data.erro) {
          setLogradouro(data.logradouro || '');
          setBairro(data.bairro || '');
          setCidade(data.localidade || '');
          setEstado(data.uf || '');

          const fetchCoord = async (query: string) => {
            try {
              const r = await fetch(
                `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`,
                { headers: { 'User-Agent': 'eQuadras-App/1.0' } }
              );
              if (r.ok) return await r.json();
            } catch {
              return [];
            }
            return [];
          };

          let nominatimData = await fetchCoord(
            `${data.logradouro}, ${data.bairro}, ${data.localidade}, ${data.uf || 'SP'}, Brasil`
          );
          if (!nominatimData || nominatimData.length === 0) {
            nominatimData = await fetchCoord(
              `${data.logradouro}, ${data.localidade}, ${data.uf || 'SP'}, Brasil`
            );
          }
          if (!nominatimData || nominatimData.length === 0) {
            nominatimData = await fetchCoord(
              `${data.bairro}, ${data.localidade}, ${data.uf || 'SP'}, Brasil`
            );
          }

          if (nominatimData && nominatimData.length > 0) {
            setLatitude(parseFloat(nominatimData[0].lat));
            setLongitude(parseFloat(nominatimData[0].lon));
          }
        }
      } catch (err) {
        console.error('Erro ao buscar CEP', err);
      } finally {
        setLoadingCep(false);
      }
    }
  }, []);

  const handleValorChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '');
    if (value === '') {
      setValorHora('');
      return;
    }
    const decimalValue = (parseInt(value, 10) / 100).toFixed(2);
    setValorHora(decimalValue);
  }, []);

  const handleDiaToggle = useCallback((dia: DiaSemana) => {
    setHorarios((prev) => ({
      ...prev,
      [dia]: {
        ...prev[dia],
        ativo: !prev[dia].ativo,
      },
    }));
  }, []);

  const handleHorarioChange = useCallback((dia: DiaSemana, field: 'horaInicio' | 'horaFim', value: string) => {
    setHorarios((prev) => ({
      ...prev,
      [dia]: {
        ...prev[dia],
        [field]: value,
      },
    }));
  }, []);

  const copiarSegParaTodos = useCallback(() => {
    const seg = horarios.MONDAY;
    setHorarios({
      MONDAY: { ...seg },
      TUESDAY: { ...seg },
      WEDNESDAY: { ...seg },
      THURSDAY: { ...seg },
      FRIDAY: { ...seg },
      SATURDAY: { ...seg },
      SUNDAY: { ...seg },
    });
  }, [horarios.MONDAY]);

  const aplicarPadraoTodos = useCallback(() => {
    setHorarios(DEFAULT_HORARIOS);
  }, []);

  const handleFileChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const selectedFiles = Array.from(e.target.files);
      const totalFotos = fotosExistentes.length + novasFotos.length + selectedFiles.length;
      if (totalFotos > 5) {
        setFeedback({ type: 'error', message: 'Você pode ter no máximo 5 fotos por quadra.' });
        return;
      }
      setNovasFotos((prev) => [...prev, ...selectedFiles]);
      const previews = selectedFiles.map((file) => URL.createObjectURL(file));
      setNovasFotosPreviews((prev) => [...prev, ...previews]);
    }
  }, [fotosExistentes.length, novasFotos.length, setFeedback]);

  const removerNovaFoto = useCallback((index: number) => {
    const urlToRemove = novasFotosPreviews[index];
    if (urlToRemove) {
      URL.revokeObjectURL(urlToRemove);
    }
    setNovasFotos((prev) => prev.filter((_, i) => i !== index));
    setNovasFotosPreviews((prev) => prev.filter((_, i) => i !== index));
  }, [novasFotosPreviews]);

  const removerFotoExistente = useCallback(async (fotoUrl: string) => {
    if (!user || !editandoId) {
      setFotosExistentes((prev) => prev.filter((f) => f !== fotoUrl));
      return;
    }
    try {
      await quadraApi.removerFoto(editandoId, fotoUrl);
      setFotosExistentes((prev) => prev.filter((f) => f !== fotoUrl));
      setFeedback({ type: 'success', message: 'Foto removida com sucesso!' });
      await onSuccessAction?.();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Erro ao remover foto.' });
    }
  }, [user, editandoId, setFeedback, onSuccessAction]);

  const handleSalvarQuadra = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    const acaoTexto = editandoId ? 'salvar as alterações da quadra' : 'cadastrar a nova quadra';
    const acaoTitulo = editandoId ? 'Confirmar Edição de Quadra' : 'Confirmar Cadastro de Quadra';

    setConfirmModal({
      isOpen: true,
      title: acaoTitulo,
      description: `Deseja realmente ${acaoTexto} "${nome}" com valor de R$ ${parseFloat(valorHora || '0').toFixed(2)}/hora?`,
      isDestructive: false,
      onConfirm: async () => {
        setConfirmModal({
          isOpen: false,
          title: '',
          description: '',
          isDestructive: false,
          onConfirm: () => {},
        });
        setLoadingMessage(editandoId ? 'Atualizando dados e fotos da quadra...' : 'Cadastrando nova quadra e enviando fotos...');
        setLoading(true);
        setFeedback(null);

        try {
          const disponibilidades: DisponibilidadeDia[] = DIAS_SEMANA
            .filter((dia) => horarios[dia.key].ativo)
            .map((dia) => ({
              diaSemana: dia.key,
              horaInicio: `${horarios[dia.key].horaInicio}:00`,
              horaFim: `${horarios[dia.key].horaFim}:00`,
            }));

          const payload = {
            nome,
            tipoEsporte,
            valorHora: parseFloat(valorHora),
            descricao,
            dataLimiteAgendamento: dataLimiteAgendamento || undefined,
            fotos: fotosExistentes,
            cep,
            logradouro,
            bairro,
            cidade,
            estado,
            latitude,
            longitude,
            disponibilidades,
          };

          let quadraSalva: Quadra;
          if (editandoId) {
            quadraSalva = await quadraApi.editar(editandoId, payload);
          } else {
            quadraSalva = await quadraApi.cadastrar(payload);
          }

          if (novasFotos.length > 0 && quadraSalva.id_quadra) {
            await quadraApi.uploadFotos(quadraSalva.id_quadra, novasFotos);
          }

          setFeedback({
            type: 'success',
            message: `Quadra "${nome}" ${editandoId ? 'atualizada' : 'cadastrada'} com sucesso!`,
          });

          fecharModal();
          await onSuccessAction?.();
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao salvar quadra.' });
        } finally {
          setLoading(false);
        }
      },
    });
  }, [
    user,
    editandoId,
    nome,
    valorHora,
    tipoEsporte,
    descricao,
    dataLimiteAgendamento,
    fotosExistentes,
    cep,
    logradouro,
    bairro,
    cidade,
    estado,
    latitude,
    longitude,
    horarios,
    novasFotos,
    setConfirmModal,
    setLoadingMessage,
    setLoading,
    setFeedback,
    fecharModal,
    onSuccessAction,
  ]);

  return {
    isModalOpen,
    setIsModalOpen,
    editandoId,
    nome,
    setNome,
    tipoEsporte,
    setTipoEsporte,
    valorHora,
    setValorHora,
    descricao,
    setDescricao,
    dataLimiteAgendamento,
    setDataLimiteAgendamento,
    cep,
    setCep,
    logradouro,
    setLogradouro,
    numero,
    setNumero,
    bairro,
    setBairro,
    cidade,
    setCidade,
    estado,
    setEstado,
    latitude,
    longitude,
    loadingCep,
    fotosExistentes,
    novasFotosPreviews,
    horarios,
    abrirModalCriacao,
    abrirModalEdicao,
    fecharModal,
    handleCepChange,
    handleValorChange,
    handleDiaToggle,
    handleHorarioChange,
    copiarSegParaTodos,
    aplicarPadraoTodos,
    handleFileChange,
    removerNovaFoto,
    removerFotoExistente,
    handleSalvarQuadra,
  };
};
