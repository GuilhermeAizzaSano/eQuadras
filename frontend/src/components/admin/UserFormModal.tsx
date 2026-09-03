import React, { useState, useEffect } from 'react';
import { Usuario, Role } from '../../types';
import { X, User, Mail, Phone, Lock, ShieldCheck, AlertCircle } from 'lucide-react';

interface UserFormModalProps {
  isOpen: boolean;
  usuarioParaEditar: Usuario | null;
  onClose: () => void;
  onSalvar: (dados: {
    nome_usuario: string;
    email_usuario: string;
    phone_usuario: string;
    role: Role;
    senha_usuario?: string;
    nova_senha?: string;
  }) => Promise<void>;
}

export const UserFormModal: React.FC<UserFormModalProps> = ({
  isOpen,
  usuarioParaEditar,
  onClose,
  onSalvar,
}) => {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState<Role>('CLIENT');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const isMaster = usuarioParaEditar?.email_usuario?.toLowerCase() === 'gui@gmail.com';

  useEffect(() => {
    if (usuarioParaEditar) {
      setNome(usuarioParaEditar.nome_usuario);
      setEmail(usuarioParaEditar.email_usuario);
      setPhone(usuarioParaEditar.phone_usuario || '');
      setRole(usuarioParaEditar.role);
      setSenha('');
    } else {
      setNome('');
      setEmail('');
      setPhone('');
      setRole('CLIENT');
      setSenha('');
    }
    setErro(null);
  }, [usuarioParaEditar, isOpen]);

  if (!isOpen) return null;

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let val = e.target.value.replace(/\D/g, '');
    if (val.length > 11) val = val.substring(0, 11);
    let formatted = val;
    if (val.length > 2) {
      formatted = `(${val.substring(0, 2)}) `;
      if (val.length > 7) {
        formatted += `${val.substring(2, 7)}-${val.substring(7)}`;
      } else {
        formatted += val.substring(2);
      }
    }
    setPhone(formatted);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro(null);

    if (!usuarioParaEditar && senha.length < 6) {
      setErro('A senha deve ter no mínimo 6 caracteres.');
      return;
    }

    if (usuarioParaEditar && senha && senha.length < 6) {
      setErro('A nova senha deve ter no mínimo 6 caracteres.');
      return;
    }

    setLoading(true);
    try {
      if (usuarioParaEditar) {
        await onSalvar({
          nome_usuario: nome,
          email_usuario: email,
          phone_usuario: phone,
          role,
          nova_senha: senha ? senha : undefined,
        });
      } else {
        await onSalvar({
          nome_usuario: nome,
          email_usuario: email,
          phone_usuario: phone,
          role,
          senha_usuario: senha,
        });
      }
      onClose();
    } catch (err: any) {
      setErro(err.message || 'Falha ao salvar usuário');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-zinc-950 border border-zinc-800 rounded-3xl max-w-md w-full p-6 space-y-5 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-900 transition"
          title="Fechar"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="space-y-1">
          <h3 className="text-lg font-bold text-white tracking-tight">
            {usuarioParaEditar ? 'Editar Usuário' : 'Cadastrar Novo Usuário'}
          </h3>
          <p className="text-xs text-zinc-400">
            {usuarioParaEditar
              ? 'Atualize as informações do usuário no sistema'
              : 'Preencha os dados para criar uma conta de Atleta ou Administrador'}
          </p>
        </div>

        {erro && (
          <div className="p-3 rounded-xl bg-red-950/40 border border-red-900/60 text-red-400 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3.5">
          <div>
            <label className="block text-xs font-medium text-zinc-300 mb-1">
              Nome Completo
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                placeholder="Nome do usuário"
                className="w-full bg-zinc-900/80 border border-zinc-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-zinc-300 mb-1">
              E-mail
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                disabled={isMaster}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="usuario@dominio.com"
                className={`w-full bg-zinc-900/80 border border-zinc-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white transition ${
                  isMaster ? 'opacity-60 cursor-not-allowed' : ''
                }`}
              />
            </div>
            {isMaster && (
              <span className="text-[10px] text-zinc-500 mt-0.5 block">
                O e-mail da conta Master não pode ser alterado.
              </span>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-zinc-300 mb-1">
                Telefone
              </label>
              <div className="relative">
                <Phone className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={phone}
                  onChange={handlePhoneChange}
                  placeholder="(11) 99999-9999"
                  className="w-full bg-zinc-900/80 border border-zinc-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-300 mb-1">
                Perfil de Acesso
              </label>
              <div className="relative">
                <ShieldCheck className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <select
                  value={role}
                  disabled={isMaster}
                  onChange={(e) => setRole(e.target.value as Role)}
                  className={`w-full bg-zinc-900/80 border border-zinc-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white focus:outline-none focus:border-white transition ${
                    isMaster ? 'opacity-60 cursor-not-allowed' : ''
                  }`}
                >
                  <option value="CLIENT">Cliente (Atleta)</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-zinc-300 mb-1">
              {usuarioParaEditar ? 'Nova Senha (opcional)' : 'Senha de Acesso'}
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required={!usuarioParaEditar}
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                placeholder={usuarioParaEditar ? 'Deixe em branco para manter a atual' : 'Mínimo 6 caracteres'}
                className="w-full bg-zinc-900/80 border border-zinc-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white transition"
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-zinc-850">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-zinc-400 hover:text-white rounded-xl transition"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="bg-white hover:bg-zinc-200 text-black font-semibold text-xs py-2 px-4 rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {loading ? 'Salvando...' : usuarioParaEditar ? 'Salvar Alterações' : 'Criar Usuário'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
