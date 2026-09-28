import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { UserManagementList } from './UserManagementList';
import { Usuario } from '../../types';

describe('UserManagementList', () => {
  const usuarios: Usuario[] = [
    { id_usuario: 1, nome_usuario: 'Cliente Bot', email_usuario: 'bot@teste.com', phone_usuario: '17997785256', role: 'CLIENT' },
    { id_usuario: 2, nome_usuario: 'Outro Cliente', email_usuario: 'outro@teste.com', phone_usuario: '11988887777', role: 'CLIENT' },
  ];

  const props = {
    usuarios,
    loading: false,
    onRefresh: vi.fn(),
    onNovoUsuario: vi.fn(),
    onEditarUsuario: vi.fn(),
    onExcluirUsuario: vi.fn(),
  };

  it('encontra telefone guardado só com dígitos quando a busca vem com máscara', () => {
    render(<UserManagementList {...props} />);

    fireEvent.change(screen.getByPlaceholderText('Buscar por nome, e-mail ou telefone...'), {
      target: { value: '(17) 99778' },
    });

    expect(screen.getByText('Cliente Bot')).toBeDefined();
    expect(screen.queryByText('Outro Cliente')).toBeNull();
  });
});
