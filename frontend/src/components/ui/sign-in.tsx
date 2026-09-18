import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

interface SignInPageProps {
  title?: React.ReactNode;
  description?: React.ReactNode;
  heroImageSrc?: string;
  email?: string;
  password?: string;
  onEmailChange?: (value: string) => void;
  onPasswordChange?: (value: string) => void;
  onSubmit?: (event: React.FormEvent<HTMLFormElement>) => void;
  isLoading?: boolean;
}

const GlassInputWrapper = ({ children }: { children: React.ReactNode }) => (
  <div className="rounded-2xl border border-white/10 bg-white/[0.04] backdrop-blur-md transition-all duration-200 focus-within:border-emerald-500/60 focus-within:bg-emerald-500/[0.06] focus-within:ring-1 focus-within:ring-emerald-500/30">
    {children}
  </div>
);

export const SignInPage: React.FC<SignInPageProps> = ({
  title = <span className="font-light text-white tracking-tight">Bem-vindo</span>,
  description = "Acesse sua conta para continuar suas reservas",
  heroImageSrc,
  email,
  password,
  onEmailChange,
  onPasswordChange,
  onSubmit,
  isLoading = false,
}) => {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="h-[100dvh] flex flex-col md:flex-row w-[100dvw] overflow-hidden bg-black select-none font-sans">
      {/* Coluna Esquerda: Formulário de Login */}
      <section className="flex-1 flex items-center justify-center p-6 sm:p-10 z-10 bg-black/90 md:bg-black">
        <div className="w-full max-w-md">
          <div className="flex flex-col gap-6">
            <div className="space-y-2">
              <h1 className="text-3xl sm:text-4xl md:text-5xl font-bold tracking-tight text-white leading-tight">
                {title}
              </h1>
              <p className="text-sm sm:text-base text-zinc-400">
                {description}
              </p>
            </div>

            <form className="space-y-5" onSubmit={onSubmit}>
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">E-mail</label>
                <GlassInputWrapper>
                  <input
                    name="email"
                    type="email"
                    required
                    autoComplete="email"
                    value={email}
                    onChange={(e) => onEmailChange?.(e.target.value)}
                    placeholder="seu.email@exemplo.com"
                    className="w-full bg-transparent text-sm p-4 text-white placeholder:text-zinc-500 rounded-2xl focus:outline-none"
                  />
                </GlassInputWrapper>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-300">Senha</label>
                <GlassInputWrapper>
                  <div className="relative">
                    <input
                      name="password"
                      type={showPassword ? 'text' : 'password'}
                      required
                      autoComplete="current-password"
                      value={password}
                      onChange={(e) => onPasswordChange?.(e.target.value)}
                      placeholder="••••••••••••"
                      className="w-full bg-transparent text-sm p-4 pr-12 text-white placeholder:text-zinc-500 rounded-2xl focus:outline-none"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute inset-y-0 right-3 flex items-center text-zinc-400 hover:text-white transition-colors focus:outline-none cursor-pointer"
                      title={showPassword ? 'Ocultar senha' : 'Exibir senha'}
                      tabIndex={-1}
                    >
                      {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                </GlassInputWrapper>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full rounded-2xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 disabled:cursor-not-allowed py-4 font-semibold text-black transition-colors duration-200 cursor-pointer shadow-lg shadow-emerald-500/20 active:scale-[0.99]"
              >
                {isLoading ? 'Entrando...' : 'Entrar'}
              </button>
            </form>

            <div className="pt-6 border-t border-white/[0.08] flex items-center justify-between text-xs text-zinc-500">
              <span>eQuadras</span>
              <span>Ambiente seguro</span>
            </div>
          </div>
        </div>
      </section>

      {/* Coluna Direita: Imagem de Hero (Quadra) */}
      {heroImageSrc && (
        <section className="hidden md:block flex-1 relative p-4 h-full bg-black">
          <div
            className="absolute inset-4 rounded-3xl bg-cover bg-center border border-white/10 shadow-2xl overflow-hidden"
            style={{ backgroundImage: `url(${heroImageSrc})` }}
          >
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/20 pointer-events-none" />
          </div>
        </section>
      )}
    </div>
  );
};
