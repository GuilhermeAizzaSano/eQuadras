import React, { useState } from 'react';
import { Usuario, Role } from '../../types';
import { UserPlus, Edit, Trash2, Shield, User, Search, RefreshCw } from 'lucide-react';
import { Badge, ConfirmModal } from '../ui';

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
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-zinc-900/60 p-4 rounded-2xl border border-zinc-800/80">
        <div className="flex items-center gap-2 flex-1 max-w-md">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar por nome, e-mail ou telefone..."
              value={filtro}
              onChange={(e) => setFiltro(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-zinc-700"
            />
          </div>

          <select
            value={roleFiltro}
            onChange={(e) => setRoleFiltro(e.target.value as any)}
            className="bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-zinc-300 focus:outline-none focus:border-zinc-700"
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
            className="p-2.5 rounded-xl border border-zinc-800 bg-zinc-950 hover:bg-zinc-900 text-zinc-400 hover:text-white transition disabled:opacity-50"
            title="Atualizar lista"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={onNovoUsuario}
            className="bg-white hover:bg-zinc-200 text-black font-semibold text-xs py-2.5 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-sm"
          >
            <UserPlus className="w-4 h-4" />
            <span>Novo Usuário</span>
          </button>
        </div>
      </div>

      {/* Tabela de Usuários */}
      <div className="bg-zinc-950 border border-zinc-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-zinc-300">
            <thead className="bg-zinc-900/80 text-zinc-400 uppercase tracking-wider font-semibold border-b border-zinc-800 text-[10px]">
              <tr>
                <th className="py-3.5 px-4">Usuário</th>
                <th className="py-3.5 px-4">E-mail</th>
                <th className="py-3.5 px-4">Telefone</th>
                <th className="py-3.5 px-4">Perfil</th>
                <th className="py-3.5 px-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-850">
              {loading && usuarios.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-zinc-500">
                    Carregando usuários...
                  </td>
                </tr>
              ) : usuariosFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-zinc-500">
                    Nenhum usuário encontrado.
                  </td>
                </tr>
              ) : (
                usuariosFiltrados.map((u) => {
                  const isMaster = u.email_usuario.toLowerCase() === 'gui@gmail.com';
                  return (
                    <tr key={u.id_usuario} className="hover:bg-zinc-900/40 transition">
                      <td className="py-3.5 px-4 font-medium text-white flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300">
                          {u.role === 'ADMIN' ? (
                            <Shield className="w-4 h-4 text-amber-400" />
                          ) : (
                            <User className="w-4 h-4 text-zinc-400" />
                          )}
                        </div>
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span>{u.nome_usuario}</span>
                            {isMaster && (
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-800/80 font-mono font-semibold">
                                MASTER
                              </span>
                            )}
                          </div>
                          <span className="text-[10px] text-zinc-500">ID: #{u.id_usuario}</span>
                        </div>
                      </td>

                      <td className="py-3.5 px-4 text-zinc-300 font-mono text-[11px]">
                        {u.email_usuario}
                      </td>

                      <td className="py-3.5 px-4 text-zinc-400 font-mono text-[11px]">
                        {u.phone_usuario || 'Não informado'}
                      </td>

                      <td className="py-3.5 px-4">
                        <Badge variant={u.role === 'ADMIN' ? 'warning' : 'neutral'}>
                          {u.role === 'ADMIN' ? 'Administrador' : 'Cliente'}
                        </Badge>
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => onEditarUsuario(u)}
                            className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-900 transition"
                            title="Editar usuário"
                          >
                            <Edit className="w-4 h-4" />
                          </button>

                          {!isMaster ? (
                            <button
                              onClick={() => setUsuarioParaExcluir(u)}
                              className="p-1.5 rounded-lg text-zinc-500 hover:text-red-400 hover:bg-red-950/30 transition"
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
