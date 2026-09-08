import React, { useState, useEffect } from 'react';
import { Usuario, Role } from '../../types';
import { X, User, Mail, Phone, Lock, AlertCircle } from 'lucide-react';
import { Button, Input, Select } from '../ui';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-surface-950/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-surface-900 border border-surface-800 rounded-3xl max-w-md w-full p-6 sm:p-7 space-y-5 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-surface-800 transition cursor-pointer"
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
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2 font-mono">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Nome Completo"
            required
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            placeholder="Nome do usuário"
            leftIcon={<User className="w-4 h-4 text-zinc-500" />}
          />

          <div>
            <Input
              label="E-mail"
              type="email"
              required
              disabled={isMaster}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="usuario@dominio.com"
              leftIcon={<Mail className="w-4 h-4 text-zinc-500" />}
            />
            {isMaster && (
              <span className="text-[10px] text-zinc-500 mt-1 block font-mono">
                O e-mail da conta Master não pode ser alterado.
              </span>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Telefone"
              required
              value={phone}
              onChange={handlePhoneChange}
              placeholder="(11) 99999-9999"
              leftIcon={<Phone className="w-4 h-4 text-zinc-500" />}
            />

            <Select
              label="Perfil de Acesso"
              value={role}
              disabled={isMaster}
              onChange={(e) => setRole(e.target.value as Role)}
            >
              <option value="CLIENT">Cliente (Atleta)</option>
              <option value="ADMIN">Administrador</option>
            </Select>
          </div>

          <Input
            label={usuarioParaEditar ? 'Nova Senha (opcional)' : 'Senha de Acesso'}
            type="password"
            required={!usuarioParaEditar}
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            placeholder={usuarioParaEditar ? 'Deixe em branco para manter a atual' : 'Mínimo 6 caracteres'}
            leftIcon={<Lock className="w-4 h-4 text-zinc-500" />}
          />

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-surface-800">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="cursor-pointer"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={loading}
              className="cursor-pointer font-bold"
            >
              {loading ? 'Salvando...' : usuarioParaEditar ? 'Salvar Alterações' : 'Criar Usuário'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
