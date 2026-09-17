package com.agendamentos.equadras.config;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AdminMasterInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminMasterInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String masterEmail;
    private final String masterPassword;

    public AdminMasterInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.master.email:gui@gmail.com}") String masterEmail,
            @Value("${admin.master.password:}") String masterPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.masterEmail = masterEmail != null ? masterEmail.trim().toLowerCase() : "";
        this.masterPassword = masterPassword != null ? masterPassword.trim() : "";
    }

    @Override
    public void run(String... args) {
        if (masterEmail.isBlank()) {
            log.warn("admin.master.email não configurado. Inicialização do Master Admin ignorada.");
            return;
        }

        var adminOpt = usuarioRepository.findByEmail_usuario(masterEmail);
        if (adminOpt.isEmpty()) {
            String senhaInicial = !masterPassword.isBlank()
                    ? masterPassword
                    : gerarSenhaTemporariaSegura();

            Usuario masterAdmin = Usuario.builder()
                    .nome_usuario("Administrador Geral")
                    .email_usuario(masterEmail)
                    .senha_usuario(passwordEncoder.encode(senhaInicial))
                    .phone_usuario("11999999999")
                    .role(Role.ADMIN)
                    .ativo(true)
                    .build();

            usuarioRepository.save(masterAdmin);
            log.info("Conta de Administrador Geral ({}) criada com sucesso.", masterEmail);
            if (masterPassword.isBlank()) {
                log.warn("ATENÇÃO: Senha gerada aleatoriamente para o Master Admin (ADMIN_MASTER_PASSWORD não definida). Altere imediatamente via aplicação.");
            }
        } else {
            Usuario masterAdmin = adminOpt.get();
            if (masterAdmin.getRole() != Role.ADMIN || !masterAdmin.isAtivo()) {
                masterAdmin.setRole(Role.ADMIN);
                masterAdmin.setAtivo(true);
                usuarioRepository.save(masterAdmin);
                log.info("Perfil ADMIN reassegurado para a conta de Administrador Geral ({}). Senha preservada.", masterEmail);
            }
        }
    }

    private String gerarSenhaTemporariaSegura() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

