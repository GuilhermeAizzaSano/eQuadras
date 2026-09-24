import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useTema } from './useTema';

const CHAVE = 'equadras-tema';

function mockMatchMedia(matchesLight: boolean) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: query.includes('light') ? matchesLight : false,
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })) as unknown as typeof window.matchMedia;
}

describe('useTema', () => {
  beforeEach(() => {
    document.documentElement.classList.remove('light');
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('sem valor salvo, segue a preferência do sistema (claro)', () => {
    mockMatchMedia(true);
    const { result } = renderHook(() => useTema());
    expect(result.current.tema).toBe('claro');
    expect(document.documentElement.classList.contains('light')).toBe(true);
  });

  it('sem valor salvo, segue a preferência do sistema (escuro)', () => {
    mockMatchMedia(false);
    const { result } = renderHook(() => useTema());
    expect(result.current.tema).toBe('escuro');
    expect(document.documentElement.classList.contains('light')).toBe(false);
  });

  it('alternar() troca a classe light e grava no localStorage', () => {
    mockMatchMedia(false);
    const { result } = renderHook(() => useTema());

    act(() => result.current.alternar());

    expect(result.current.tema).toBe('claro');
    expect(document.documentElement.classList.contains('light')).toBe(true);
    expect(localStorage.getItem(CHAVE)).toBe('claro');

    act(() => result.current.alternar());

    expect(result.current.tema).toBe('escuro');
    expect(document.documentElement.classList.contains('light')).toBe(false);
    expect(localStorage.getItem(CHAVE)).toBe('escuro');
  });

  it('respeita o valor salvo mesmo que difira da preferência do sistema', () => {
    mockMatchMedia(true);
    localStorage.setItem(CHAVE, 'escuro');

    const { result } = renderHook(() => useTema());

    expect(result.current.tema).toBe('escuro');
    expect(document.documentElement.classList.contains('light')).toBe(false);
  });

  it('localStorage lançando exceção não quebra o hook', () => {
    mockMatchMedia(false);
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });

    const { result } = renderHook(() => useTema());
    expect(result.current.tema).toBe('escuro');

    act(() => result.current.alternar());
    expect(result.current.tema).toBe('claro');

    getItemSpy.mockRestore();
    setItemSpy.mockRestore();
  });
});
