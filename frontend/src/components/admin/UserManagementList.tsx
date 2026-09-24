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
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-surface-2/80 backdrop-blur-xl p-4 rounded-2xl sm:rounded-3xl border border-fg/[0.1]">
        <div className="flex items-center gap-2.5 flex-1 max-w-md">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-fg/60 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar por nome, e-mail ou telefone..."
              value={filtro}
              onChange={(e) => setFiltro(e.target.value)}
              className="w-full bg-fg/[0.03] border border-fg/[0.1] rounded-xl pl-9 pr-3 py-2 text-xs text-fg placeholder-fg/30 focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition font-sans"
            />
          </div>

          <select
            value={roleFiltro}
            onChange={(e) => setRoleFiltro(e.target.value as Role | 'TODOS')}
            className="bg-surface-3 border border-fg/[0.1] rounded-xl px-3 py-2 text-xs text-fg/80 focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition cursor-pointer"
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
            className="p-2.5 rounded-xl border border-fg/[0.1] bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/60 hover:text-fg transition disabled:opacity-40 cursor-pointer"
            title="Atualizar lista" aria-label="Atualizar lista"
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
      <div className="bg-surface-2 border border-fg/[0.1] rounded-2xl sm:rounded-3xl overflow-hidden shadow-[inset_0_1px_0_0_rgba(255,255,255,0.06)]">
        <div className="overflow-x-auto min-h-[420px]">
          <table className="w-full text-left text-xs text-fg/80 table-fixed min-w-[800px]">
            <colgroup>
              <col className="w-[260px]" />
              <col className="w-[240px]" />
              <col className="w-[160px]" />
              <col className="w-[140px]" />
              <col className="w-[100px]" />
            </colgroup>
            <thead className="bg-fg/[0.03] text-fg/60 uppercase tracking-wider font-semibold border-b border-fg/[0.06] text-xs font-mono">
              <tr>
                <th className="py-3.5 px-5">Usuário</th>
                <th className="py-3.5 px-4">E-mail</th>
                <th className="py-3.5 px-4">Telefone</th>
                <th className="py-3.5 px-4">Perfil</th>
                <th className="py-3.5 px-5 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-fg/[0.06]">
              {loading && usuarios.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-20 text-center text-fg/60 font-mono text-xs">
                    Carregando usuários...
                  </td>
                </tr>
              ) : usuariosFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-20 text-center text-fg/60 font-mono text-xs">
                    Nenhum usuário encontrado.
                  </td>
                </tr>
              ) : (
                usuariosFiltrados.map((u) => {
                  const isMaster = u.masterAdmin === true || u.email_usuario.toLowerCase() === 'gui@gmail.com';
                  return (
                    <tr key={u.id_usuario} className="h-[56px] hover:bg-fg/[0.03] transition">
                      <td className="py-3.5 px-5 font-medium text-fg flex items-center gap-3 truncate">
                        <div className="w-8 h-8 rounded-xl bg-fg/[0.03] border border-fg/[0.1] flex items-center justify-center text-fg/70 shrink-0">
                          {u.role === 'ADMIN' ? (
                            <Shield className="w-4 h-4 text-warning" />
                          ) : (
                            <User className="w-4 h-4 text-info" />
                          )}
                        </div>
                        <div className="truncate">
                          <div className="flex items-center gap-1.5 font-semibold text-sm truncate">
                            <span className="truncate" title={u.nome_usuario}>{u.nome_usuario}</span>
                            {isMaster && (
                              <span className="text-xs px-1.5 py-0.5 rounded-full bg-info/15 text-info border border-info/30 font-mono font-medium shrink-0">
                                MASTER
                              </span>
                            )}
                          </div>
                          <span className="text-xs text-fg/60 font-mono">ID: #{u.id_usuario}</span>
                        </div>
                      </td>

                      <td className="py-3.5 px-4 text-fg/70 font-mono text-xs truncate" title={u.email_usuario}>
                        {u.email_usuario}
                      </td>

                      <td className="py-3.5 px-4 text-fg/60 font-mono text-xs">
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
                            className="p-1.5 rounded-lg text-fg/60 hover:text-fg hover:bg-fg/[0.1] transition cursor-pointer"
                            title="Editar usuário"
                          >
                            <Edit className="w-4 h-4" />
                          </button>

                          {!isMaster ? (
                            <button
                              onClick={() => setUsuarioParaExcluir(u)}
                              className="p-1.5 rounded-lg text-fg/60 hover:text-danger hover:bg-danger/10 transition cursor-pointer"
                              title="Excluir usuário"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          ) : (
                            <span className="p-1.5 opacity-20 cursor-not-allowed" title="Conta master não pode ser excluída">
                              <Trash2 className="w-4 h-4 text-fg/60" />
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
