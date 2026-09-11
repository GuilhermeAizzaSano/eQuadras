import React, { useState } from 'react';
import { Usuario, Role } from '../../types';
import { UserPlus, Edit, Trash2, Shield, User, Search, RefreshCw } from 'lucide-react';
import { Badge, ConfirmModal, Button } from '../ui';

interface UserManagementListProps {
  usuarios: Usuario[];
  loading: boolean;
  onRefresh: () => void;
  onNovoUsuario: () => void;
  onEditarUsuario: (usuario: Usuario) => void;
  onExcluirUsuario: (usuario: Usuario) => void;
}

export const UserManagementList: React.FC<UserManagementListProps> = ({
  usuarios,
  loading,
  onRefresh,
  onNovoUsuario,
  onEditarUsuario,
  onExcluirUsuario,
}) => {
  const [filtro, setFiltro] = useState('');
  const [roleFiltro, setRoleFiltro] = useState<Role | 'TODOS'>('TODOS');
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<Usuario | null>(null);

  const usuariosFiltrados = usuarios.filter((u) => {
    const atendeRole = roleFiltro === 'TODOS' || u.role === roleFiltro;
    const termo = filtro.toLowerCase();
    const atendeTexto =
      u.nome_usuario.toLowerCase().includes(termo) ||
      u.email_usuario.toLowerCase().includes(termo) ||
      (u.phone_usuario && u.phone_usuario.includes(termo));
    return atendeRole && atendeTexto;
  });

  return (
    <div className="space-y-4">
      {/* Barra de Ações Superior */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-surface-900 p-4 rounded-2xl border border-surface-800">
        <div className="flex items-center gap-2.5 flex-1 max-w-md">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar por nome, e-mail ou telefone..."
              value={filtro}
              onChange={(e) => setFiltro(e.target.value)}
              className="w-full bg-surface-950 border border-surface-750 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-brand-400 transition font-sans"
            />
          </div>

          <select
            value={roleFiltro}
            onChange={(e) => setRoleFiltro(e.target.value as Role | 'TODOS')}
            className="bg-surface-950 border border-surface-750 rounded-xl px-3 py-2 text-xs text-zinc-300 focus:outline-none focus:border-brand-400 transition cursor-pointer font-mono"
          >
            <option value="TODOS">Todos os Perfis</option>
            <option value="ADMIN">Administradores</option>
            <option value="CLIENT">Clientes (Atletas)</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={onRefresh}
            disabled={loading}
            className="p-2.5 rounded-xl border border-surface-800 bg-surface-950 hover:bg-surface-850 text-zinc-400 hover:text-white transition disabled:opacity-50 cursor-pointer"
            title="Atualizar lista"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <Button
            variant="primary"
            onClick={onNovoUsuario}
            className="cursor-pointer"
          >
            <UserPlus className="w-4 h-4" />
            <span>Novo Usuário</span>
          </Button>
        </div>
      </div>

      {/* Tabela de Usuários */}
      <div className="bg-surface-900 border border-surface-800 rounded-3xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-zinc-300">
            <thead className="bg-surface-950/80 text-zinc-400 uppercase tracking-wider font-bold border-b border-surface-800 text-[10px] font-mono">
              <tr>
                <th className="py-3.5 px-5">Usuário</th>
                <th className="py-3.5 px-4">E-mail</th>
                <th className="py-3.5 px-4">Telefone</th>
                <th className="py-3.5 px-4">Perfil</th>
                <th className="py-3.5 px-5 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-surface-800/60">
              {loading && usuarios.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-zinc-500 font-mono text-xs">
                    Carregando usuários...
                  </td>
                </tr>
              ) : usuariosFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-zinc-500 font-mono text-xs">
                    Nenhum usuário encontrado.
                  </td>
                </tr>
              ) : (
                usuariosFiltrados.map((u) => {
                  const isMaster = u.email_usuario.toLowerCase() === 'gui@gmail.com';
                  return (
                    <tr key={u.id_usuario} className="hover:bg-surface-850/50 transition">
                      <td className="py-3.5 px-5 font-medium text-white flex items-center gap-3">
                        <div className="w-8 h-8 rounded-xl bg-surface-950 border border-surface-800 flex items-center justify-center text-zinc-300">
                          {u.role === 'ADMIN' ? (
                            <Shield className="w-4 h-4 text-amber-400" />
                          ) : (
                            <User className="w-4 h-4 text-brand-400" />
                          )}
                        </div>
                        <div>
                          <div className="flex items-center gap-1.5 font-bold text-sm">
                            <span>{u.nome_usuario}</span>
                            {isMaster && (
                              <span className="text-[9px] px-1.5 py-0.2 rounded-full bg-brand-500/15 text-brand-400 border border-brand-500/30 font-mono font-bold">
                                MASTER
                              </span>
                            )}
                          </div>
                          <span className="text-[10px] text-zinc-500 font-mono">ID: #{u.id_usuario}</span>
                        </div>
                      </td>

                      <td className="py-3.5 px-4 text-zinc-300 font-mono text-[11px]">
                        {u.email_usuario}
                      </td>

                      <td className="py-3.5 px-4 text-zinc-400 font-mono text-[11px]">
                        {u.phone_usuario || 'Não informado'}
                      </td>

                      <td className="py-3.5 px-4">
                        <Badge variant={u.role === 'ADMIN' ? 'warning' : 'neutral'} withDot>
                          {u.role === 'ADMIN' ? 'Administrador' : 'Cliente'}
                        </Badge>
                      </td>

                      <td className="py-3.5 px-5 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => onEditarUsuario(u)}
                            className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-surface-800 transition cursor-pointer"
                            title="Editar usuário"
                          >
                            <Edit className="w-4 h-4" />
                          </button>

                          {!isMaster ? (
                            <button
                              onClick={() => setUsuarioParaExcluir(u)}
                              className="p-1.5 rounded-lg text-zinc-500 hover:text-rose-400 hover:bg-rose-500/10 transition cursor-pointer"
                              title="Excluir usuário"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          ) : (
                            <span className="p-1.5 opacity-20 cursor-not-allowed" title="Conta master não pode ser excluída">
                              <Trash2 className="w-4 h-4 text-zinc-600" />
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal de Confirmação de Exclusão */}
      <ConfirmModal
        isOpen={!!usuarioParaExcluir}
        title="Excluir Usuário"
        description={`Tem certeza que deseja remover o usuário "${usuarioParaExcluir?.nome_usuario}" (${usuarioParaExcluir?.email_usuario})? Esta ação não pode ser desfeita.`}
        confirmLabel="Excluir Usuário"
        cancelLabel="Cancelar"
        isDestructive={true}
        onConfirm={() => {
          if (usuarioParaExcluir) {
            onExcluirUsuario(usuarioParaExcluir);
            setUsuarioParaExcluir(null);
          }
        }}
        onCancel={() => setUsuarioParaExcluir(null)}
      />
    </div>
  );
};
