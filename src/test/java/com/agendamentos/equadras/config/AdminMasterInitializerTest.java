package com.agendamentos.equadras.config;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** R4: a criação do Master Admin não pode derrubar o boot por colisão de telefone. */
class AdminMasterInitializerTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private AdminMasterInitializer inicializador() {
        return new AdminMasterInitializer(usuarioRepository, passwordEncoder, "master@teste.com", "senha", "11988880000");
    }

    @Test
    @DisplayName("R4: telefone livre cria o master com o telefone configurado")
    void telefoneLivreCriaMaster() {
        when(usuarioRepository.findByEmail_usuario("master@teste.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByPhone_usuario("11988880000")).thenReturn(Optional.empty());

        inicializador().run();

        verify(usuarioRepository).save(argThat(u -> "11988880000".equals(u.getPhone_usuario())));
    }

    @Test
    @DisplayName("R4: telefone já usado não cria o master e não lança exceção")
    void telefoneEmUsoNaoDerrubaBoot() {
        when(usuarioRepository.findByEmail_usuario("master@teste.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByPhone_usuario("11988880000")).thenReturn(Optional.of(new Usuario()));

        assertDoesNotThrow(() -> inicializador().run());

        verify(usuarioRepository, never()).save(any());
    }
}
