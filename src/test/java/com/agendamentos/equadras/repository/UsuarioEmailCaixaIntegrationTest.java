package com.agendamentos.equadras.repository;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** E-mail é comparado sem distinção de maiúsculas, como o índice uk_usuarios_email_lower. */
@SpringBootTest
class UsuarioEmailCaixaIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;

    private Usuario usuario;

    @AfterEach
    void limpar() {
        usuarioRepository.delete(usuario);
    }

    @Test
    @DisplayName("Busca e existência de e-mail ignoram maiúsculas")
    void buscaIgnoraCaixa() {
        usuario = usuarioRepository.save(Usuario.builder().nome_usuario("Ana Caixa").email_usuario("ana.caixa@teste.com")
                .senha_usuario("x").phone_usuario("11999990021").role(Role.CLIENT).build());

        assertTrue(usuarioRepository.existsByEmail_usuario("ANA.Caixa@Teste.com"));
        assertTrue(usuarioRepository.findByEmail_usuario("ANA.Caixa@Teste.com").isPresent());
    }
}
