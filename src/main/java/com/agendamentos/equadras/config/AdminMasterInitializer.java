package com.agendamentos.equadras.config;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminMasterInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminMasterInitializer.class);

    public static final String MASTER_EMAIL = "gui@gmail.com";
    public static final String MASTER_DEFAULT_PASSWORD = "uw57k$0B982e";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminMasterInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        var adminOpt = usuarioRepository.findByEmail_usuario(MASTER_EMAIL);
        if (adminOpt.isEmpty()) {
            Usuario masterAdmin = Usuario.builder()
                    .nome_usuario("Administrador Geral")
                    .email_usuario(MASTER_EMAIL)
                    .senha_usuario(passwordEncoder.encode(MASTER_DEFAULT_PASSWORD))
                    .phone_usuario("11999999999")
                    .role(Role.ADMIN)
                    .build();

            usuarioRepository.save(masterAdmin);
            log.info("Conta de Administrador Geral ({}) criada com sucesso.", MASTER_EMAIL);
        } else {
            Usuario masterAdmin = adminOpt.get();
            boolean atualizou = false;
            if (masterAdmin.getRole() != Role.ADMIN) {
                masterAdmin.setRole(Role.ADMIN);
                atualizou = true;
            }
            // Sincroniza senha se necessário
            if (!passwordEncoder.matches(MASTER_DEFAULT_PASSWORD, masterAdmin.getSenha_usuario())) {
                masterAdmin.setSenha_usuario(passwordEncoder.encode(MASTER_DEFAULT_PASSWORD));
                atualizou = true;
            }
            if (atualizou) {
                usuarioRepository.save(masterAdmin);
                log.info("Conta de Administrador Geral ({}) atualizada com credenciais e perfil ADMIN.", MASTER_EMAIL);
            }
        }
    }
}
