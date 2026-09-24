import { useCallback, useEffect, useState } from 'react';

export type Tema = 'claro' | 'escuro';

const CHAVE_ARMAZENAMENTO = 'equadras-tema';

function lerTemaSalvo(): Tema | null {
  try {
    const valor = localStorage.getItem(CHAVE_ARMAZENAMENTO);
    return valor === 'claro' || valor === 'escuro' ? valor : null;
  } catch {
    return null;
  }
}

function preferenciaDoSistema(): Tema {
  try {
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'claro' : 'escuro';
  } catch {
    return 'escuro';
  }
}

function aplicarTema(tema: Tema) {
  document.documentElement.classList.toggle('light', tema === 'claro');
}

/**
 * Estado do tema claro/escuro. Ao montar, honra o valor salvo em localStorage
 * ou, na ausência dele, a preferência do sistema (prefers-color-scheme).
 * Falhas de acesso ao localStorage (modo privado, storage bloqueado) não
 * quebram o hook: o tema simplesmente não persiste entre sessões.
 */
export function useTema() {
  const [tema, setTema] = useState<Tema>(() => lerTemaSalvo() ?? preferenciaDoSistema());

  useEffect(() => {
    aplicarTema(tema);
  }, [tema]);

  const definir = useCallback((novoTema: Tema) => {
    setTema(novoTema);
    try {
      localStorage.setItem(CHAVE_ARMAZENAMENTO, novoTema);
    } catch {
      // Armazenamento indisponível: o tema vale só para esta sessão.
    }
  }, []);

  const alternar = useCallback(() => {
    setTema((atual) => {
      const novoTema: Tema = atual === 'claro' ? 'escuro' : 'claro';
      try {
        localStorage.setItem(CHAVE_ARMAZENAMENTO, novoTema);
      } catch {
        // Armazenamento indisponível: o tema vale só para esta sessão.
      }
      return novoTema;
    });
  }, []);

  return { tema, alternar, definir };
}
