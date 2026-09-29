package com.agendamentos.equadras.config;

import com.agendamentos.equadras.config.ExemplosDaApi.Erros;
import com.agendamentos.equadras.config.ExemplosDaApi.ExemploOperacao;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.agendamentos.equadras.config.ExemplosDaApi.chave;
import static com.agendamentos.equadras.config.ExemplosDaApi.operacao;

final class ExemplosUsuarios {

    private static final String USUARIO = """
        {
          "id_usuario": 10,
          "nome_usuario": "Arthur Prado",
          "email_usuario": "arthur.prado@email.com",
          "phone_usuario": "(11) 99999-8888",
          "role": "CLIENT",
          "criadoEm": "2026-09-04T10:00:00",
          "masterAdmin": false
        }
        """;

    private static final String USUARIO_CRIADO = """
        {
          "id_usuario": 12,
          "nome_usuario": "Carlos Eduardo",
          "email_usuario": "carlos.eduardo@email.com",
          "phone_usuario": "(11) 98888-7777",
          "role": "CLIENT",
          "criadoEm": "2026-09-29T09:15:00",
          "masterAdmin": false
        }
        """;

    private ExemplosUsuarios() {
    }

    static Map<String, ExemploOperacao> exemplos() {
        Map<String, ExemploOperacao> m = new LinkedHashMap<>();

        m.put(chave("POST", "/api/usuarios/login"), operacao()
                .request("""
                    {
                      "email_usuario": "arthur.prado@email.com",
                      "senha_usuario": "SenhaSegura@123"
                    }
                    """)
                .okComCabecalhos("200", "Login realizado. O corpo traz o perfil; o JWT vai apenas no cookie HttpOnly `equadras_session`.",
                        USUARIO, "UsuarioResponseDTO", Map.of("Set-Cookie", "Cookie de sessão `equadras_session` (HttpOnly, SameSite=Lax, Path=/)."))
                .erro(Erros.validacao("Dados inválidos: E-mail: Formato de e-mail inválido"))
                .erro(Erros.regraNegocio("CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos."))
                .erro(Erros.muitasTentativasLogin("Muitas tentativas falhas de login para esta conta. Tente novamente em 300 segundos."))
                .build());

        m.put(chave("POST", "/api/usuarios/logout"), operacao()
                .semCorpoComCabecalhos("204", "Sessão encerrada. Sem corpo; o cookie de sessão é expirado.",
                        Map.of("Set-Cookie", "Cookie `equadras_session` com Max-Age=0, removendo a sessão do navegador."))
                .build());

        m.put(chave("GET", "/api/usuarios/me"), operacao()
                .ok("200", "Perfil do usuário autenticado", USUARIO)
                .erro(Erros.naoAutenticado())
                .build());

        m.put(chave("GET", "/api/usuarios/api-key"), operacao()
                .ok("200", "Metadados da chave de API (nunca a chave em texto plano)", """
                    {
                      "possuiChave": true,
                      "last4": "a1b2",
                      "criadaEm": "2026-09-20T12:00:00Z",
                      "ultimoUsoEm": "2026-09-28T18:45:10Z"
                    }
                    """)
                .erro(Erros.naoAutenticado())
                .build());

        m.put(chave("POST", "/api/usuarios/api-key/regenerar"), operacao()
                .okComCabecalhos("200", "Nova chave emitida. A chave em texto plano só aparece nesta resposta.", """
                    {
                      "apiKey": "eq_9f8e7d6c5b4a39281706f5e4d3c2b1a0",
                      "last4": "b1a0",
                      "criadaEm": "2026-09-29T14:30:00Z"
                    }
                    """, "ApiKeyCriadaDTO", Map.of("Cache-Control", "`no-store, no-cache, must-revalidate, max-age=0`: a resposta não pode ser guardada em cache."))
                .erro(Erros.naoAutenticado())
                .erro(Erros.muitasRegeneracoesApiKey())
                .build());

        m.put(chave("DELETE", "/api/usuarios/api-key"), operacao()
                .semCorpo("204", "Chave revogada (operação idempotente: sem chave ativa também devolve 204)")
                .erro(Erros.naoAutenticado())
                .build());

        m.put(chave("POST", "/api/usuarios"), operacao()
                .request("""
                    {
                      "nome_usuario": "Carlos Eduardo",
                      "email_usuario": "carlos.eduardo@email.com",
                      "senha_usuario": "SenhaForte@123",
                      "phone_usuario": "(11) 98888-7777",
                      "role": "CLIENT"
                    }
                    """)
                .ok("201", "Usuário cadastrado", USUARIO_CRIADO)
                .erro(Erros.validacao("Dados inválidos: E-mail: Formato de e-mail inválido"))
                .erro(Erros.regraNegocio("EMAIL_DUPLICADO", "E-mail já cadastrado no sistema."))
                .erro(Erros.conflito("TELEFONE_EM_USO", "Telefone já cadastrado para outro usuário."))
                .erro(Erros.acessoNegado("Acesso restrito ao Administrador Geral do sistema."))
                .build());

        m.put(chave("GET", "/api/usuarios"), operacao()
                .ok("200", "Página de usuários (com `page`). Sem `page` o código devolve uma lista simples de até 200 usuários.", """
                    {
                      "content": [
                        {
                          "id_usuario": 10,
                          "nome_usuario": "Arthur Prado",
                          "email_usuario": "arthur.prado@email.com",
                          "phone_usuario": "(11) 99999-8888",
                          "role": "CLIENT",
                          "criadoEm": "2026-09-04T10:00:00",
                          "masterAdmin": false
                        }
                      ],
                      "page": 0,
                      "size": 10,
                      "totalElements": 1,
                      "totalPages": 1
                    }
                    """)
                .erro(Erros.acessoNegado("Acesso restrito ao Administrador Geral do sistema."))
                .build());

        m.put(chave("PUT", "/api/usuarios/{id}"), operacao()
                .request("""
                    {
                      "nome_usuario": "Arthur Prado Silva",
                      "email_usuario": "arthur.silva@email.com",
                      "phone_usuario": "(11) 99999-9999",
                      "role": "CLIENT",
                      "nova_senha": "NovaSenha@456"
                    }
                    """)
                .ok("200", "Usuário atualizado", """
                    {
                      "id_usuario": 10,
                      "nome_usuario": "Arthur Prado Silva",
                      "email_usuario": "arthur.silva@email.com",
                      "phone_usuario": "(11) 99999-9999",
                      "role": "CLIENT",
                      "criadoEm": "2026-09-04T10:00:00",
                      "masterAdmin": false
                    }
                    """)
                .erro(Erros.validacao("Dados inválidos: Nome: O nome deve ter entre 3 e 80 caracteres"))
                .erro(Erros.naoEncontrado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado para o ID: 99"))
                .erro(Erros.regraNegocio("EMAIL_DUPLICADO", "E-mail já cadastrado por outro usuário."))
                .erro(Erros.regraNegocio("OPERACAO_NAO_PERMITIDA", "O e-mail do Administrador Geral não pode ser modificado."))
                .erro(Erros.acessoNegado("Acesso restrito ao Administrador Geral do sistema."))
                .build());

        m.put(chave("DELETE", "/api/usuarios/{id}"), operacao()
                .semCorpo("204", "Usuário excluído")
                .erro(Erros.naoEncontrado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado para o ID: 99"))
                .erro(Erros.regraNegocio("OPERACAO_NAO_PERMITIDA", "A conta do Administrador Geral não pode ser excluída."))
                .erro(Erros.acessoNegado("Acesso restrito ao Administrador Geral do sistema."))
                .build());

        m.put(chave("PATCH", "/api/usuarios/minha-senha"), operacao()
                .request("""
                    {
                      "senhaAtual": "SenhaVelha@123",
                      "novaSenha": "SenhaSuperSegura@456"
                    }
                    """)
                .semCorpo("204", "Senha alterada. Sem corpo.")
                .erro(Erros.validacao("Dados inválidos: novaSenha: A nova senha deve ter no mínimo 6 caracteres, incluindo 1 letra maiúscula, 1 minúscula, 1 número e 1 caractere especial/símbolo."))
                .erro(Erros.regraNegocio("SENHA_INCORRETA", "A senha atual informada está incorreta."))
                .erro(Erros.regraNegocio("SENHA_REPETIDA", "A nova senha deve ser diferente da senha atual."))
                .build());

        m.put(chave("GET", "/api/usuarios/{id}"), operacao()
                .ok("200", "Dados do usuário", USUARIO)
                .erro(Erros.naoEncontrado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado para o ID: 99"))
                .erro(Erros.acessoNegado("Você não tem permissão para visualizar os dados de outro usuário."))
                .build());

        return m;
    }
}
