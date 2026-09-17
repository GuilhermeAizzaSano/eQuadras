package com.agendamentos.equadras.security;

import com.agendamentos.equadras.dto.response.ApiKeyCriadaDTO;
import com.agendamentos.equadras.dto.response.ApiKeyInfoDTO;
import com.agendamentos.equadras.model.entity.AuditoriaApiKey;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import com.agendamentos.equadras.repository.AuditoriaApiKeyRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaApiKeyRepository auditoriaApiKeyRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id_usuario(10L)
                .nome_usuario("Dev Teste")
                .email_usuario("dev@teste.com")
                .role(Role.CLIENT)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Deve gerar ApiKey com prefixo eq_, tamanho 46 e caracteres de alta entropia")
    void deveGerarApiKeyComFormatoCorreto() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyCriadaDTO criadaDTO = apiKeyService.gerarOuRegenerar(10L, "127.0.0.1", "JUnit5");

        assertNotNull(criadaDTO);
        assertNotNull(criadaDTO.apiKey());
        assertTrue(criadaDTO.apiKey().startsWith("eq_"), "Deve iniciar com o prefixo 'eq_'");
        assertEquals(46, criadaDTO.apiKey().length(), "Prefixo 'eq_' (3) + 43 caracteres Base64Url = 46 caracteres");
    }

    @Test
    @DisplayName("Deve calcular hash SHA-256 em 64 caracteres hexadecimais")
    void deveCalcularSha256Hex() {
        String token = "eq_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        String hash = ApiKeyService.sha256Hex(token);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "Hash SHA-256 deve possuir exatamente 64 caracteres hexadecimais");
        assertTrue(hash.matches("^[0-9a-f]{64}$"), "Hash SHA-256 deve conter apenas dígitos hexadecimais minúsculos");
    }

    @Test
    @DisplayName("Deve regenerar chave, persistir hash e gravar auditoria GERADA quando não tinha chave anterior")
    void deveRegenerarChavePelaPrimeiraVez() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyCriadaDTO criadaDTO = apiKeyService.gerarOuRegenerar(10L, "192.168.1.1", "AgentTest");

        assertNotNull(criadaDTO);
        assertNotNull(criadaDTO.apiKey());
        assertTrue(criadaDTO.apiKey().startsWith("eq_"));
        assertEquals(4, criadaDTO.last4().length());
        assertEquals(criadaDTO.apiKey().substring(criadaDTO.apiKey().length() - 4), criadaDTO.last4());

        // Valida que o usuário recebeu o hash e last4
        assertEquals(criadaDTO.last4(), usuario.getApiKeyLast4());
        assertNotNull(usuario.getApiKeyHash());
        assertNotNull(usuario.getApiKeyCriadaEm());

        // Valida registro de auditoria com evento GERADA
        ArgumentCaptor<AuditoriaApiKey> captor = ArgumentCaptor.forClass(AuditoriaApiKey.class);
        verify(auditoriaApiKeyRepository, times(1)).save(captor.capture());
        AuditoriaApiKey auditoria = captor.getValue();
        assertEquals(10L, auditoria.getUsuarioId());
        assertEquals("GERADA", auditoria.getEvento());
        assertEquals("192.168.1.1", auditoria.getIp());
    }

    @Test
    @DisplayName("Deve regenerar chave e gravar auditoria REGENERADA quando já possuía chave anterior")
    void deveRegenerarChaveQuandoJaPossuiaAnterior() {
        usuario.setApiKeyHash("hash_antigo_1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        usuario.setApiKeyLast4("9999");
        usuario.setApiKeyCriadaEm(Instant.now().minusSeconds(86400));

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyCriadaDTO criadaDTO = apiKeyService.gerarOuRegenerar(10L, "10.0.0.1", "Mozilla");

        assertNotNull(criadaDTO);
        assertNotEquals("9999", criadaDTO.last4());
        assertNotEquals("hash_antigo_1234567890abcdef1234567890abcdef1234567890abcdef1234567890", usuario.getApiKeyHash());

        ArgumentCaptor<AuditoriaApiKey> captor = ArgumentCaptor.forClass(AuditoriaApiKey.class);
        verify(auditoriaApiKeyRepository, times(1)).save(captor.capture());
        AuditoriaApiKey auditoria = captor.getValue();
        assertEquals("REGENERADA", auditoria.getEvento());
    }

    @Test
    @DisplayName("Deve revogar chave, limpar hash e registrar auditoria REVOGADA")
    void deveRevogarChave() {
        usuario.setApiKeyHash("hash_existente");
        usuario.setApiKeyLast4("1234");
        usuario.setApiKeyCriadaEm(Instant.now());
        usuario.setApiKeyUltimoUsoEm(Instant.now());

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        apiKeyService.revogar(10L, "127.0.0.1", "Chrome");

        assertNull(usuario.getApiKeyHash());
        verify(usuarioRepository, times(1)).save(usuario);

        ArgumentCaptor<AuditoriaApiKey> captor = ArgumentCaptor.forClass(AuditoriaApiKey.class);
        verify(auditoriaApiKeyRepository, times(1)).save(captor.capture());
        AuditoriaApiKey auditoria = captor.getValue();
        assertEquals(10L, auditoria.getUsuarioId());
        assertEquals("REVOGADA", auditoria.getEvento());
    }

    @Test
    @DisplayName("Deve consultar metadados da chave corretamente (com chave ativa)")
    void deveConsultarMetadadosComChave() {
        Instant agora = Instant.now();
        usuario.setApiKeyHash("hash_ativo");
        usuario.setApiKeyLast4("5678");
        usuario.setApiKeyCriadaEm(agora.minusSeconds(3600));
        usuario.setApiKeyUltimoUsoEm(agora.minusSeconds(600));

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        ApiKeyInfoDTO info = apiKeyService.info(10L);

        assertNotNull(info);
        assertTrue(info.possuiChave());
        assertEquals("5678", info.last4());
        assertEquals(usuario.getApiKeyCriadaEm(), info.criadaEm());
        assertEquals(usuario.getApiKeyUltimoUsoEm(), info.ultimoUsoEm());
    }

    @Test
    @DisplayName("Deve consultar metadados da chave corretamente (sem chave ativa)")
    void deveConsultarMetadadosSemChave() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        ApiKeyInfoDTO info = apiKeyService.info(10L);

        assertNotNull(info);
        assertFalse(info.possuiChave());
        assertNull(info.last4());
        assertNull(info.criadaEm());
        assertNull(info.ultimoUsoEm());
    }

    @Test
    @DisplayName("Deve autenticar com sucesso para hash válido e usuário ativo")
    void deveAutenticarApiKeyValida() {
        String rawToken = "eq_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        String hash = ApiKeyService.sha256Hex(rawToken);

        when(usuarioRepository.findByApiKeyHash(hash)).thenReturn(Optional.of(usuario));

        Optional<Usuario> autenticado = apiKeyService.autenticar(rawToken);

        assertTrue(autenticado.isPresent());
        assertEquals(10L, autenticado.get().getId_usuario());
    }

    @Test
    @DisplayName("Não deve autenticar se o formato da chave for inválido")
    void naoDeveAutenticarChaveInvalida() {
        Optional<Usuario> result1 = apiKeyService.autenticar("token_sem_prefixo");
        assertTrue(result1.isEmpty());

        Optional<Usuario> result2 = apiKeyService.autenticar(null);
        assertTrue(result2.isEmpty());

        Optional<Usuario> result3 = apiKeyService.autenticar("eq_curto");
        assertTrue(result3.isEmpty());
    }

    @Test
    @DisplayName("Não deve autenticar se o usuário estiver inativo (ativo = false)")
    void naoDeveAutenticarUsuarioInativo() {
        usuario.setAtivo(false);
        String rawToken = "eq_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        String hash = ApiKeyService.sha256Hex(rawToken);

        when(usuarioRepository.findByApiKeyHash(hash)).thenReturn(Optional.of(usuario));

        Optional<Usuario> autenticado = apiKeyService.autenticar(rawToken);

        assertTrue(autenticado.isEmpty(), "Usuário inativo não deve ser autenticado");
    }

    @Test
    @DisplayName("Deve chamar atualizarUltimoUsoComThrottling delegando limites temporais para o repositório")
    void deveRegistrarUsoComThrottling() {
        apiKeyService.registrarUso(10L);
        verify(usuarioRepository, times(1)).atualizarUltimoUsoComThrottling(eq(10L), any(Instant.class), any(Instant.class));
    }
}
