# ADR 004: Adoção de Java 21 Virtual Threads

- **Status:** Proposto — retroativo
- **Data:** 2026-09-23
- **Decisores:** Time de Desenvolvimento eQuadras
- **Domínios Afetados:** A1 (Backend Structure), A3 (Concurrency/Realtime), A5 (Infra & Operations)

---

## 1. Contexto

A plataforma roda em uma máquina virtual modesta (1 OCPU, 1 GB RAM). O modelo tradicional do Tomcat baseado em threads de plataforma pré-alocadas consome memória de stack significativa (~1 MB por thread), limitando o número de conexões HTTP concorrentes sustentáveis.

## 2. Decisão

Habilitou-se **Virtual Threads do Java 21** via propriedade `spring.threads.virtual.enabled=true` no Spring Boot em conjunto com o Tomcat 11 embutido.

## 3. Consequências e Riscos Identificados

### Positivas
- **Alta Eficiência de Threads:** Threads leves gerenciadas pela JVM com footprint de memória desprezível por conexão HTTP em espera de I/O.
- **Eliminação de Pools Rígidos no Tomcat:** Permite que centenas de conexões HTTP de clientes lentos ou SSE aguardem sem esgotar o pool de threads do servidor web.

### Negativas / Riscos Revelados na Auditoria
- **Transferência do Gargalo para o HikariCP (A2/A3):** Sem o limite rígido de threads de plataforma do Tomcat, milhares de requisições simultâneas podem avançar imediatamente até a camada de serviço e disputar as 5 conexões do pool do HikariCP, estourando o `connection-timeout=20000ms` sob picos de tráfego. Falta um mecanismo de bulkhead ou semáforo pré-banco.
- **Risco de Pinning de Carrier Threads (A3):** Em Java 21, blocos `synchronized` contendo operações bloqueantes de I/O realizam *pinning* da Virtual Thread à Carrier Thread do SO, degradando o scheduler da JVM. Mapeou-se uso de `synchronized` no `RateLimitFilter`.
- **Incompatibilidade com Retenção Transacional em SSE (A3):** A chamada `@Transactional` segurando transações abertas durante transmissões SSE trava os recursos da JVM e do banco.
