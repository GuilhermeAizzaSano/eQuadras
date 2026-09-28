import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type React from 'react';
import { useCourtForm } from './useCourtForm';
import { quadraApi } from '../api/apiClient';
import { Quadra } from '../types';

vi.mock('../api/apiClient', () => ({
  quadraApi: {
    cadastrar: vi.fn(),
    editar: vi.fn(),
    uploadFotos: vi.fn(),
    removerFoto: vi.fn(),
  },
}));

const api = vi.mocked(quadraApi);

const quadra = (parcial: Partial<Quadra>): Quadra => ({
  id_quadra: 7,
  nome: 'Quadra Teste',
  tipoEsporte: 'FUTSAL',
  valorHora: 100,
  ativa: true,
  fotos: [],
  disponibilidades: [],
  versao: 3,
  ...parcial,
});

const montar = () => {
  let onConfirm: () => void | Promise<void> = () => {};
  const setConfirmModal = vi.fn((modal: { isOpen: boolean; onConfirm: () => void }) => {
    if (modal.isOpen) onConfirm = modal.onConfirm;
  });
  const hook = renderHook(() =>
    useCourtForm({
      user: { id: 1 },
      onSuccessAction: vi.fn(),
      setFeedback: vi.fn(),
      setLoading: vi.fn(),
      setLoadingMessage: vi.fn(),
      setConfirmModal,
    }),
  );
  const salvar = async () => {
    await act(async () => {
      await hook.result.current.handleSalvarQuadra({ preventDefault: () => {} } as React.FormEvent);
    });
    await act(async () => {
      await onConfirm();
    });
  };
  const adicionarFoto = () => {
    act(() => {
      hook.result.current.handleFileChange({
        target: { files: [new File(['x'], 'foto.jpg', { type: 'image/jpeg' })] },
      } as unknown as React.ChangeEvent<HTMLInputElement>);
    });
  };
  return { hook, salvar, adicionarFoto };
};

describe('useCourtForm: versão da quadra', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    URL.createObjectURL = vi.fn(() => 'blob:preview');
    URL.revokeObjectURL = vi.fn();
  });

  it('edição: depois de falhar o upload, o novo envio usa a versão devolvida pelo PUT', async () => {
    api.editar.mockResolvedValue(quadra({ versao: 4 }));
    api.uploadFotos.mockRejectedValueOnce(new Error('upload falhou')).mockResolvedValueOnce(quadra({ versao: 5 }));
    const { hook, salvar, adicionarFoto } = montar();
    act(() => hook.result.current.abrirModalEdicao(quadra({ versao: 3 })));
    adicionarFoto();

    await salvar();
    await salvar();

    expect(api.editar).toHaveBeenNthCalledWith(1, 7, expect.objectContaining({ versao: 3 }));
    expect(api.editar).toHaveBeenNthCalledWith(2, 7, expect.objectContaining({ versao: 4 }));
  });

  it('criação: depois de falhar o upload, o novo envio edita a quadra criada em vez de cadastrar de novo', async () => {
    api.cadastrar.mockResolvedValue(quadra({ id_quadra: 10, versao: 0 }));
    api.editar.mockResolvedValue(quadra({ id_quadra: 10, versao: 1 }));
    api.uploadFotos.mockRejectedValueOnce(new Error('upload falhou')).mockResolvedValueOnce(quadra({ id_quadra: 10, versao: 2 }));
    const { hook, salvar, adicionarFoto } = montar();
    act(() => hook.result.current.abrirModalCriacao());
    adicionarFoto();

    await salvar();
    await salvar();

    expect(api.cadastrar).toHaveBeenCalledTimes(1);
    expect(api.editar).toHaveBeenCalledWith(10, expect.objectContaining({ versao: 0 }));
  });

  it('remover foto atualiza a versão usada no próximo envio', async () => {
    api.removerFoto.mockResolvedValue(quadra({ versao: 5, fotos: [] }));
    api.editar.mockResolvedValue(quadra({ versao: 6 }));
    const { hook, salvar } = montar();
    act(() => hook.result.current.abrirModalEdicao(quadra({ versao: 4, fotos: ['/uploads/a.jpg'] })));

    await act(async () => {
      await hook.result.current.removerFotoExistente('/uploads/a.jpg');
    });
    await salvar();

    expect(api.editar).toHaveBeenCalledWith(7, expect.objectContaining({ versao: 5 }));
  });
});
