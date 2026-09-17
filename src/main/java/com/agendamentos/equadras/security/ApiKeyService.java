package com.agendamentos.equadras.security;

import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.dto.response.ApiKeyInfoDTO;
import com.agendamentos.equadras.model.entity.AuditoriaApiKey;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.AuditoriaApiKeyRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^eq_[A-Za-z0-9_-]{43}$");

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaApiKeyRepository auditoriaRepository;

    public ApiKeyService(UsuarioRepository usuarioRepository, AuditoriaApiKeyRepository auditoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public ApiKeyCriadaDTO gerarOuRegenerar(Long usuarioId, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para ID: " + usuarioId));

        if (!usuario.isAtivo()) {
            throw new IllegalStateException("Usuário inativo não pode gerar API-KEY.");
        }

        String evento = (usuario.getApiKeyHash() == null) ? "GERADA" : "REGENERADA";

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomStr = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawApiKey = "eq_" + randomStr;

        String hash = sha256Hex(rawApiKey);
        String last4 = rawApiKey.substring(rawApiKey.length() - 4);
        Instant agora = Instant.now();

        usuario.setApiKeyHash(hash);
        usuario.setApiKeyLast4(last4);
        usuario.setApiKeyCriadaEm(agora);
        usuario.setApiKeyUltimoUsoEm(null);
        usuarioRepository.save(usuario);

        auditoriaRepository.save(new AuditoriaApiKey(usuarioId, evento, ip, userAgent, agora));

        log.info("API-KEY evento={} realizado para usuarioId={}, ip={}", evento, usuarioId, ip);

        return new ApiKeyCriadaDTO(rawApiKey, last4, agora);
    }

    @Transactional
    public void revogar(Long usuarioId, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para ID: " + usuarioId));

        if (usuario.getApiKeyHash() == null) {
            // No-op idempotente se o usuário não possui chave ativa
            return;
        }

        usuario.setApiKeyHash(null);
        // Preserva last4, criadaEm e ultimoUsoEm para fins de auditoria
        usuarioRepository.save(usuario);

        Instant agora = Instant.now();
        auditoriaRepository.save(new AuditoriaApiKey(usuarioId, "REVOGADA", ip, userAgent, agora));

        log.info("API-KEY evento=REVOGADA realizado para usuarioId={}, ip={}", usuarioId, ip);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> autenticar(String rawKey) {
        if (rawKey == null || !API_KEY_PATTERN.matcher(rawKey).matches()) {
            return Optional.empty();
        }

        String hash = sha256Hex(rawKey);
        return usuarioRepository.findByApiKeyHash(hash)
                .filter(Usuario::isAtivo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarUso(Long usuarioId) {
        Instant agora = Instant.now();
        Instant limite = agora.minus(Duration.ofMinutes(5));
        usuarioRepository.atualizarUltimoUsoComThrottling(usuarioId, agora, limite);
    }

    @Transactional(readOnly = true)
    public ApiKeyInfoDTO info(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para ID: " + usuarioId));

        boolean possuiChave = usuario.getApiKeyHash() != null;
        if (!possuiChave) {
            return new ApiKeyInfoDTO(false, null, null, null);
        }

        return new ApiKeyInfoDTO(true, usuario.getApiKeyLast4(), usuario.getApiKeyCriadaEm(), usuario.getApiKeyUltimoUsoEm());
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 não disponível", e);
        }
    }
}
