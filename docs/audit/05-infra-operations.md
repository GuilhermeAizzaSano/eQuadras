# 05 — Infraestrutura e Operação

> **Auditoria de Arquitetura — Subagente A5**  
> **Escopo:** Infraestrutura, VM OCI, JVM, systemd, Nginx, TLS, Processos de Deploy, Backup e Observabilidade.  
> **Status da Execução:** Somente Leitura (acesso SSH à VM OCI `137.131.163.62` e inspeção estática do repositório).

---

## 1. Escopo Coberto

- **VM e JVM:** Shape da VM OCI (`VM.Standard.E2.1.micro` vs A1 Ampere), capacidade de recursos (CPU, RAM, disco, swap), dimensionamento de heap (`-Xms`, `-Xmx`, garbage collector), flags de tolerância a falhas (`ExitOnOutOfMemoryError`, `HeapDumpOnOutOfMemoryError`), riscos da política de ociosidade da OCI Always Free.
- **systemd:** Configuração do serviço `equadras-backend.service`, usuário de execução, políticas de reinício (`Restart`), gerenciamento de segredos via `EnvironmentFile`, sandboxing/hardening (`NoNewPrivileges`, `ProtectSystem`, `ProtectHome`, `PrivateTmp`), graceful shutdown (`server.shutdown`).
- **Nginx e TLS:** Configuração de proxy reverso (`/etc/nginx/sites-enabled/equadras`), suporte a SSE (`proxy_buffering`, timeouts, `proxy_http_version`), headers de encaminhamento e identificação de IP real (`real_ip` vs Cloudflare), limite de corpo de requisições (`client_max_body_size`), compressão (`gzip`), certificados TLS e ciclo de renovação do Certbot.
- **Operação e Resiliência:** Ponto único de falha (SPOF) e cálculo de RTO real vs meta da Fase 0, processo de build e deploy (`.github/workflows/ci.yml` vs scripts shell), política de backup vs RPO da Fase 0, riscos de pausamento no Supabase Free Tier, observabilidade (Actuator, Micrometer, journald, monitoramento sintético) e segurança de acesso ao SO (SSH, `unattended-upgrades`, firewall local).

---

## 2. Itens do Checklist

| Categoria | Item de Auditoria | Status | Resumo / Evidência Principal |
|---|---|:---:|---|
| **VM & JVM** | Shape da VM vs consumo real de CPU e memória | ⚠️ Parcial | `VM.Standard.E2.1.micro` (1 OCPU AMD / 2 vCPUs, 954 MiB RAM). Memória livre baixa (84 MiB livre, 409 MiB disp.), swap em uso ativo (343 MiB). |
| **VM & JVM** | Heap dimensionado deixando folga para SO e Nginx | ⚠️ Parcial | `-Xms128m -Xmx320m -XX:+UseG1GC`. JVM RSS atual em ~155 MiB; no limite para evitar OOM quando concorre com Maven ou Nginx. |
| **VM & JVM** | Flags de HeapDump e Exit em OOM | ❌ Não conforme | Flags `-XX:+HeapDumpOnOutOfMemoryError` e `-XX:+ExitOnOutOfMemoryError` ausentes na inicialização da JVM. |
| **VM & JVM** | Política de recuperação de instâncias ociosas da OCI | ⚠️ Parcial | `sar -u` apontou CPU idle médio de 98.94% (uso real ~1.06%, p95 < 5%). Risco iminente de desprovisionamento por ociosidade. |
| **systemd** | Serviço executado por usuário dedicado não-root | ⚠️ Parcial | Executa como `User=ubuntu` (usuário interativo com sudo completo), não como usuário de sistema isolado (`nologin`). |
| **systemd** | `Restart=on-failure` com `RestartSec` | ⚠️ Parcial | Configurado com `Restart=always` e `RestartSec=10`. Funcional para manter no ar, mas impede shutdown limpo (`exit 0`). |
| **systemd** | Segredos via `EnvironmentFile` com permissão 600 | ❌ Não conforme | `/etc/default/equadras-backend` possui permissão `644` (leitura pública local); `/home/ubuntu/setup_vm.sh` contém senha do DB em texto puro (`775`). |
| **systemd** | Hardening e sandboxing no unit | ❌ Não conforme | `NoNewPrivileges`, `ProtectSystem`, `ProtectHome` e `PrivateTmp` desativados (`no`). Sem restrição de `ReadWritePaths`. |
| **systemd** | Graceful shutdown (`server.shutdown=graceful`) | ❌ Não conforme | Spring Boot usa o padrão `immediate`. `server.shutdown=graceful` ausente em `application.properties`. |
| **Nginx & TLS** | Configuração de SSE e buffering | ⚠️ Parcial | `proxy_buffering off` e `proxy_read_timeout 86400s` presentes, porém aplicados globalmente a todas as APIs; `proxy_http_version 1.1` ausente. |
| **Nginx & TLS** | `client_max_body_size` e ranges `real_ip` da Cloudflare | ❌ Não conforme | `client_max_body_size` ausente (default 1MB quebra upload de fotos); `set_real_ip_from` ausente (rate limiter bloqueia IP do proxy Cloudflare). |
| **Nginx & TLS** | Certificado TLS e renovação automática | ✅ Conforme | Let's Encrypt ECDSA válido por 70 dias; renovação periódica via `certbot.timer`; `certbot renew --dry-run` bem-sucedido. |
| **Operação** | Ponto único de falha (SPOF) e RTO | ❌ Não conforme | Instância única sem réplica, sem IaC (Terraform). RTO estimado em 2 a 4+ horas em caso de pane, violando a meta de RTO ≤ 2h. |
| **Operação** | Processo de deploy reproduzível e automatizado | ❌ Não conforme | CI/CD compila Maven com testes pulados na VM de produção de 1GB; script de front executa `sed -i` em arquivos TypeScript no servidor. |
| **Operação** | Política de backup vs RPO (24h) | ❌ Não conforme | Supabase Free tier sem backups automáticos/PITR; nenhum cron/timer na VM para `pg_dump`; RPO de 24h não garantido. |
| **Operação** | Risco de inatividade no Supabase Free | ⚠️ Parcial | Projetos Supabase Free são pausados após 7 dias sem requisições HTTP; o backend usa apenas JDBC direto/pooler. |
| **Operação** | Observabilidade e métricas (Actuator, logs) | ❌ Não conforme | Actuator ausente (`pom.xml`), sem health check HTTP, sem JSON logging estruturado, sem monitoramento sintético externo. |
| **Operação** | Atualizações de SO, SSH e firewall | ⚠️ Parcial | `unattended-upgrades` ativo, SSH com chave pública, porém `PermitRootLogin without-password` ativo e portas 80/443 abertas a qualquer IP. |

---

## 3. Achados Consolidados

### [INFRA-01] [🔴 Crítico] Ponto Único de Falha Total (SPOF) e RTO Incompatível com a Meta da Fase 0
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** VM OCI única (`137.131.163.62`), `00-context.md` (Topologia C4 Container), ausência de código de infraestrutura como código (IaC).
  ```bash
  # Uptime e host único
  uptime: 15:24:03 up 19 days, 23:34, 2 users, load average: 0.08, 0.09, 0.07
  ```
- **Problema:** Toda a camada de computação do sistema (Nginx, SPA e Spring Boot) depende exclusivamente de uma única instância de computação `VM.Standard.E2.1.micro`. Não existe ambiente secundário, standby passivo, nem automação de provisionamento (Terraform, OpenTofu, Ansible ou Docker Compose).
- **Impacto:** **Disponibilidade e Confiabilidade.** Em caso de falha de hardware no datacenter da Oracle, término de instância Always Free ou corrupção do disco de boot, o processo de restauração dependerá de configuração manual artesanal (instalar pacotes, configurar Nginx, compilar projetos, gerar certificados Let's Encrypt e restaurar arquivos de ambiente). O RTO real excederá com facilidade **2 a 4 horas**, descumprindo a meta acordada na Fase 0 (RTO ≤ 2 horas).
- **Recomendação:** Modularizar a aplicação em contêineres Docker (ou criar um playbook Ansible de provisionamento automatizado) e publicar imagens de contêiner imutáveis em registry, permitindo subir a stack inteira em qualquer nova VM em minutos.
- **Esforço:** M
- **Relacionados:** INFRA-10

---

### [INFRA-02] [🔴 Crítico] Ausência de Backups Automatizados do Banco de Dados (Descumprimento da Meta de RPO)
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Saída do comando de inspeção de agendamentos do sistema operacional:
  ```bash
  $ crontab -l; sudo crontab -l; systemctl list-timers
  no crontab for ubuntu
  no crontab for root
  # Nenhum timer de backup ativo entre os 15 timers do systemd
  ```
- **Problema:** O banco de dados de produção está hospedado no **Supabase Free Tier**. Nesta modalidade, o Supabase **não disponibiliza** Point-in-Time Recovery (PITR) e não retém backups gerenciados diários para download ou restauração rápida. Simultaneamente, não há na VM da OCI nem no repositório GitHub nenhum script automatizado (`cron`, `systemd.timer` ou GitHub Action) que execute `pg_dump` e envie os dados para um destino seguro externo (ex: AWS S3, Cloudflare R2 ou OCI Object Storage). O repositório contém apenas arquivos `.sql` locais e desatualizados (`backup_full.sql`, `backup_equadras.sql`).
- **Impacto:** **Perda de Dados e Confiabilidade.** Caso ocorra exclusão involuntária de tabelas, execução indevida de migração Flyway, corrupção lógica ou exclusão do projeto gratuito por inatividade na plataforma do Supabase, todos os cadastros, reservas e registros financeiros serão perdidos sem possibilidade de recuperação recente, violando a meta de **RPO ≤ 24 horas**.
- **Recomendação:** Implementar imediatamente um job diário via GitHub Actions (ou `systemd.timer` em máquina externa) que realize `pg_dump` com compressão e criptografia, armazenando os últimos 14 a 30 dias em bucket de Object Storage com custo zero (ex: Cloudflare R2 ou OCI Object Storage Always Free).
- **Esforço:** P
- **Relacionados:** INFRA-06

---

### [INFRA-03] [🔴 Crítico] Segredos Expostos com Permissão 644 e Senhas em Texto Puro em Scripts Shell na VM
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Verificação de permissões do arquivo de ambiente e inspeção de scripts utilitários na pasta home:
  ```bash
  $ sudo stat -c "%a %U %G %n" /etc/default/equadras-backend
  644 root root /etc/default/equadras-backend

  $ ls -la /home/ubuntu/setup_vm.sh
  -rwxrwxr-x 1 ubuntu ubuntu 2474 Sep  3 16:40 /home/ubuntu/setup_vm.sh
  ```
  Trecho identificado em `/home/ubuntu/setup_vm.sh`:
  ```bash
  SPRING_DATASOURCE_PASSWORD=***
  ```
- **Problema:** 
  1. O arquivo `/etc/default/equadras-backend` possui permissão octal `644` (leitura liberada para o mundo). Qualquer processo ou usuário não-root que venha a operar na VM (ex: conta do Nginx `www-data` ou um usuário temporário) tem permissão de leitura sobre senhas de banco de dados (`SPRING_DATASOURCE_PASSWORD`) e tokens de pagamento (`MERCADOPAGO_ACCESS_TOKEN`).
  2. O script `/home/ubuntu/setup_vm.sh` possui permissão `775` e contém a senha mestra de acesso ao Supabase em texto puro hardcoded.
- **Impacto:** **Segurança e Confidencialidade.** Violação das práticas básicas de Least Privilege e defesa em profundidade. Um vazamento via local file inclusion (LFI) ou comprometimento de processo web expõe a integridade de todo o banco de dados.
- **Recomendação:** 
  1. Executar `sudo chmod 600 /etc/default/equadras-backend` e definir propriedade estrita para o usuário executor do serviço.
  2. Remover as senhas hardcoded em `/home/ubuntu/setup_vm.sh` e expurgar arquivos de script legados que contenham credenciais.
- **Esforço:** P
- **Relacionados:** INFRA-09

---

### [INFRA-04] [🟠 Alto] Bypassing da Cloudflare por Falta de `real_ip` no Nginx e Ausência de Restrição de Origem no Firewall
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Regras ativas do firewall Linux e configuração do Nginx na VM:
  ```bash
  $ sudo iptables -S | grep -E "dport (80|443)"
  -A INPUT -p tcp -m state --state NEW -m tcp --dport 443 -j ACCEPT
  -A INPUT -p tcp -m state --state NEW -m tcp --dport 80 -j ACCEPT
  ```
  Arquivo `/etc/nginx/sites-enabled/equadras`:
  ```nginx
  location ~ ^/(api|usuarios|quadras|agendamentos|notificacoes|pagamentos|auth|uploads) {
      proxy_pass http://127.0.0.1:8080;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      ...
  }
  ```
- **Problema:**
  1. **Bypass de Borda:** As portas 80 e 443 do host aceitam conexões TCP diretas de qualquer IP da internet (`0.0.0.0/0`). Um atacante que descubra o IP público da VM (`137.131.163.62`) pode enviar requisições diretas à máquina contornando completamente o WAF, proteções anti-DDoS e regras de rate limiting da Cloudflare.
  2. **Ausência de Módulo `real_ip`:** O Nginx não possui o bloco `set_real_ip_from` configurado com os prefixos de IP oficiais da Cloudflare, nem utiliza `real_ip_header CF-Connecting-IP;`. Desta forma, a variável `$remote_addr` no Nginx reflete o IP do servidor proxy da Cloudflare, e não o IP real do cliente.
  3. **Impacto no Rate Limiting do Backend:** O backend Spring Boot (`RateLimitFilter`) extrai o IP de `X-Forwarded-For` ou `X-Real-IP`. Como o Nginx repassa o IP da Cloudflare, todas as requisições de dezenas de clientes roteadas pelo mesmo edge da Cloudflare compartilham a mesma cota de requisições, gerando bloqueios indevidos (`429 Too Many Requests`) para usuários legítimos.
- **Impacto:** **Segurança e Disponibilidade.** Fragilização do perímetro contra ataques direcionados e funcionamento incorreto do rate limiter da aplicação.
- **Recomendação:**
  1. Configurar `/etc/nginx/conf.d/cloudflare.conf` importando todos os ranges IPv4/IPv6 da Cloudflare com `set_real_ip_from` e definir `real_ip_header CF-Connecting-IP;`.
  2. Restringir as portas 80 e 443 na Security List da OCI e no `iptables` da VM exclusivamente para a lista de CIDRs da Cloudflare ou habilitar Cloudflare Authenticated Origin Pulls (TLS mútuo de borda).
- **Esforço:** M
- **Relacionados:** A4 (Segurança)

---

### [INFRA-05] [🟠 Alto] Ausência Total de Observabilidade, Health Check e Métricas Operacionais
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Inspeção do `pom.xml`, teste de endpoint via SSH e configuração de logs:
  ```bash
  $ curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/actuator/health
  401

  $ grep -rn "spring-boot-starter-actuator" pom.xml
  # Retorno vazio: dependência não existe no projeto
  ```
  Arquivo `/etc/systemd/journald.conf`:
  ```ini
  [Journal]
  # Todas as diretivas comentadas, sem restrição de SystemMaxUse
  ```
- **Problema:** 
  1. A aplicação não possui **Spring Boot Actuator** nem **Micrometer**. Não há nenhum endpoint de checagem de integridade (`/actuator/health`) que valide conectividade com o banco Supabase, pool HikariCP ou estado da JVM.
  2. A tentativa de consulta a `/actuator/health` resulta em `401 Unauthorized` pelo Spring Security.
  3. Não existe agente de monitoramento sintético (ex: Uptime Kuma, BetterStack, StatusCake) monitorando a saúde da plataforma. A equipe de suporte não recebe alertas de indisponibilidade, esgotamento de conexões ou pico de erros 5xx.
  4. Os logs do Spring Boot são emitidos em texto plano padrão, sem formatação JSON estruturada (com campos como `traceId`, `spanId`, `exception_class`), dificultando auditoria e filtragem.
  5. O `journald` não possui limite máximo de retenção (`SystemMaxUse`), permitindo consumir até 10% da partição `/` (4.5 GB).
- **Impacto:** **Operabilidade e MTTR (Mean Time to Recovery).** Falhas em produção tornam-se "invisíveis" até a notificação por usuários finais. Diagnósticos de degradação de performance tornam-se lentos e dependentes de inspeção manual linha a linha via SSH.
- **Recomendação:** 
  1. Incluir `spring-boot-starter-actuator` e `micrometer-registry-prometheus` no `pom.xml`, expondo `/actuator/health` sem autenticação com probes de liveness e readiness.
  2. Configurar `logback-spring.xml` com encoder JSON estruturado.
  3. Definir `SystemMaxUse=500M` em `/etc/systemd/journald.conf`.
  4. Plugar um monitor externo gratuito de uptime (BetterStack ou Uptime Kuma) disparando alertas via webhook/Telegram/Discord.
- **Esforço:** M
- **Relacionados:** A7 (Qualidade e Entrega)

---

### [INFRA-06] [🟠 Alto] Risco de Pausamento Automático do Banco de Dados Supabase Free por Inatividade
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** `00-context.md` (item 2 e item 3.1); política de ciclo de vida do Supabase Free Tier.
- **Problema:** No plano gratuito do Supabase, instâncias que não registram atividade em sua API HTTP (PostgREST) durante **7 dias consecutivos** entram automaticamente em estado pausado (*Paused Project*). Como o eQuadras utiliza comunicação exclusiva via conexão direta JDBC e pooler Supavisor (`aws-0-sa-east-1.pooler.supabase.com:5432`), o motor de telemetria de inatividade da API do Supabase pode desconsiderar o tráfego JDBC puro e acionar o pausamento da base em períodos de baixa movimentação (ex: férias acadêmicas ou recesso).
- **Impacto:** **Disponibilidade.** O backend falhará ao tentar abrir conexões HikariCP, retornando erros 500 generalizados e exigindo intervenção manual no console do Supabase para religar a instância (*unpause* que pode levar de 3 a 5 minutos).
- **Recomendação:** Configurar uma tarefa agendada (ex: GitHub Action diária ou cron job) que execute uma chamada HTTP autenticada leve contra a API REST do Supabase (`GET /rest/v1/quadras?select=id&limit=1`) mantendo a telemetria do projeto ativa, ou efetuar upgrade para o plano Pro.
- **Esforço:** P
- **Relacionados:** INFRA-02

---

### [INFRA-07] [🟡 Médio] Risco de Desprovisionamento da Instância OCI Always Free por Ociosidade
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Histórico de consumo de CPU obtido pelo relatório do `sysstat` (`sar -u`) na VM:
  ```bash
  $ sar -u | tail -5
  15:10:18  all   0.08  0.00  0.05  0.01  0.04  99.82
  15:20:33  all   0.67  0.27  0.82  1.56  3.63  93.05
  15:30:08  all   0.58  0.00  0.38  0.42  1.01  97.61
  Average:  all   0.32  0.01  0.20  0.10  0.43  98.94
  ```
- **Problema:** A Oracle Cloud possui uma política ativa de recuperação de recursos para instâncias Always Free (*Reclamation of Idle Compute Instances*). Uma instância é formalmente classificada como ociosa se, ao longo de um intervalo móvel de 7 dias:
  1. O percentil 95 da utilização de CPU for inferior a 20%; E
  2. O consumo de rede for inferior a 20%.
  A medição real do `sar` confirma que a CPU da VM passa **98.94% do tempo ociosa** (utilização média de ~1.06%, com p95 muito abaixo de 5%).
- **Impacto:** **Disponibilidade.** A Oracle pode enviar notificação de desligamento e, após o prazo de aviso, suspender ou excluir a VM, provocando indisponibilidade total do serviço sem falha de software.
- **Recomendação:** 
  1. Efetuar upgrade da conta da Oracle Cloud para o modelo **Pay As You Go (PAYG)**. O plano PAYG mantém os limites Always Free gratuitos (R$ 0,00 de cobrança se não exceder os limites), mas **elimina por contrato** a política de recuperação de instâncias ociosas.
  2. Como paliativo técnico, implementar rotinas de verificação sintética periódica.
- **Esforço:** P
- **Relacionados:** INFRA-01

---

### [INFRA-08] [🟡 Médio] Ausência de Parâmetros de Diagnóstico e Recuperação Automática de OOM na JVM
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Linha de comando real da JVM em execução via `jcmd`:
  ```bash
  $ jcmd 290080 VM.command_line
  290080:
  VM Arguments:
  jvm_args: -Xms128m -Xmx320m -XX:+UseG1GC 
  java_command: /home/ubuntu/eQuadras/target/equadras-0.0.1-SNAPSHOT.jar
  ```
- **Problema:** A JVM está inicializada exclusivamente com `-Xms128m -Xmx320m -XX:+UseG1GC`. Não foram incluídas as seguintes flags de confiabilidade essenciais:
  1. `-XX:+HeapDumpOnOutOfMemoryError`: Sem esta flag, a causa raiz de um estouro de heap (ex: vazamento em SSE emitters ou caches) não deixa rastros para análise post-mortem.
  2. `-XX:HeapDumpPath`: Não há diretório delimitado para gravação de dumps.
  3. `-XX:+ExitOnOutOfMemoryError` (ou `-XX:+CrashOnOutOfMemoryError`): Em cenários de esgotamento de memória, o processo Java frequentemente não finaliza, entrando em loop infinito de coleta de lixo (*GC thrashing*), consumindo 100% de CPU sem conseguir responder requisições e impedindo o systemd de acionar o `Restart=always`.
- **Impacto:** **Disponibilidade e Resiliência.** A aplicação pode travar silenciosamente sem que o orquestrador do sistema operacional consiga recuperá-la.
- **Recomendação:** Atualizar o `ExecStart` no arquivo `/etc/systemd/system/equadras-backend.service`:
  ```ini
  ExecStart=/usr/bin/java -Xms128m -Xmx320m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/equadras/heapdump.hprof -XX:+ExitOnOutOfMemoryError -jar /home/ubuntu/eQuadras/target/equadras-0.0.1-SNAPSHOT.jar
  ```
- **Esforço:** P
- **Relacionados:** A3 (Concorrência e Tempo Real)

---

### [INFRA-09] [🟡 Médio] Ausência de Hardening e Sandboxing no Serviço systemd
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Propriedades de isolamento do unit do systemd inspecionadas:
  ```bash
  $ systemctl show equadras-backend -p User,NoNewPrivileges,ProtectSystem,ProtectHome,PrivateTmp
  User=ubuntu
  NoNewPrivileges=no
  ProtectSystem=no
  ProtectHome=no
  PrivateTmp=no
  ```
- **Problema:** 
  1. O serviço executa sob o usuário `ubuntu`, que é a conta interativa administrativa principal do servidor, dotada de privilégios sudo sem restrição.
  2. Todas as diretivas modernas de sandboxing do Linux e systemd estão desabilitadas (`no`), permitindo que a aplicação leia `/home/ubuntu`, acesse diretórios temporários globais (`/tmp`), eleve privilégios e altere arquivos de sistema caso uma vulnerabilidade na aplicação seja explorada.
- **Impacto:** **Segurança do Servidor.** Em caso de vulnerabilidade no backend (ex: deserialização insegura, execução de código arbitrário ou path traversal no endpoint de uploads), o invasor herda os acessos completos do usuário `ubuntu`.
- **Recomendação:** Criar um usuário de serviço de sistema dedicado sem privilégios (`useradd -r -s /usr/sbin/nologin equadras`) e habilitar as opções de sandboxing no unit:
  ```ini
  [Service]
  User=equadras
  Group=equadras
  NoNewPrivileges=true
  ProtectSystem=strict
  ProtectHome=true
  PrivateTmp=true
  ReadWritePaths=/home/ubuntu/eQuadras/uploads /var/log/equadras
  ```
- **Esforço:** M
- **Relacionados:** INFRA-03

---

### [INFRA-10] [🟡 Médio] Pipeline de Deploy com Compilação Pesada na VM de 1GB e Hot-Patching via Shell
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Inspeção de `.github/workflows/ci.yml` e scripts de deploy na VM:
  ```yaml
  # .github/workflows/ci.yml (linhas 96-99)
  echo "==> Mudanças no backend detectadas. Compilando pacote Spring Boot..."
  ./mvnw clean package -DskipTests=true
  sudo systemctl restart equadras-backend.service
  ```
  Script `/home/ubuntu/rebuild_frontend.sh`:
  ```bash
  sed -i 's|return `http://${window.location.hostname}:8080`;|return window.location.origin;|g' /home/ubuntu/eQuadras/frontend/src/api/apiClient.ts
  cd /home/ubuntu/eQuadras/frontend
  npm run build
  ```
- **Problema:**
  1. **Compilação no Host de Produção:** O pipeline de CI/CD dispara um comando SSH que executa `mvn clean package` diretamente dentro da VM de produção. Com apenas 954 MiB de memória total e a JVM rodando, o Maven + Compilador Java demandam centenas de megabytes extras de memória, elevando o uso de swap (343 MiB já alocados) e gerando risco crônico de acionamento do Linux OOM Killer contra o processo do banco ou do Spring Boot.
  2. **Testes Ignorados no Deploy:** O comando de compilação em produção roda com `-DskipTests=true`.
  3. **Hot-Patching Frágil de Código:** O script de build do frontend aplica substituição de texto com `sed -i` diretamente nos arquivos TypeScript rastreados pelo Git (`apiClient.ts`) para contornar URLs locais hardcoded. Isso torna a árvore Git suja e sujeita a quebras em futuros `git pull`.
- **Impacto:** **Confiabilidade do Deploy e Manutenibilidade.** Alto risco de quebra de deploy por esgotamento de memória e introdução de estados inconsistentes no repositório em disco na produção.
- **Recomendação:** Compilar o JAR do backend e o bundle estático do frontend dentro dos runners do GitHub Actions (que dispõem de 7 GB de RAM e ambiente limpo) e realizar o deploy copiando unicamente os artefatos prontos (`target/*.jar` e `frontend/dist/`) via `scp`/`rsync`, sem instalar ou rodar compiladores na VM.
- **Esforço:** M
- **Relacionados:** A7 (Qualidade e Entrega)

---

### [INFRA-11] [🔵 Baixo] Configuração Global Inadequada de Nginx para Uploads e SSE
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Arquivo `/etc/nginx/sites-enabled/equadras`:
  ```nginx
  location ~ ^/(api|usuarios|quadras|agendamentos|notificacoes|pagamentos|auth|uploads) {
      proxy_pass http://127.0.0.1:8080;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;

      # Suporte a SSE (Server-Sent Events) para notificacoes
      proxy_buffering off;
      proxy_cache off;
      proxy_read_timeout 86400s;
  }
  ```
- **Problema:**
  1. **Limite de Upload Ausente:** A diretiva `client_max_body_size` não está declarada no Nginx, adotando o valor padrão de **1 MB**. Quando o usuário tentar realizar upload de fotos de quadras (`POST /quadras/{id}/fotos`) tiradas por smartphones modernos (frequentemente entre 2 MB e 8 MB), o Nginx rejeitará a requisição com o código HTTP `413 Request Entity Too Large` antes mesmo de encaminhar ao Spring Boot.
  2. **HTTP/1.0 no Upstream:** As diretivas `proxy_http_version 1.1;` e `proxy_set_header Connection "";` estão ausentes. Por padrão, o Nginx conecta ao Tomcat usando HTTP/1.0, o que impede conexões persistentes *keep-alive* e impõe overhead TCP desnecessário para cada requisição.
  3. **Escopo Genérico de SSE:** As diretivas de SSE (`proxy_buffering off; proxy_read_timeout 86400s;`) foram declaradas dentro de uma expressão regular única para todas as rotas da API, desativando o buffer do Nginx inclusive para rotas REST transacionais que se beneficiariam de buffering de resposta.
- **Impacto:** **Experiência do Usuário e Desempenho.** Falhas em uploads de fotos de quadras e conexões menos eficientes com o servidor de aplicação.
- **Recomendação:**
  1. Inserir `client_max_body_size 10M;` no bloco `server` ou na rota de upload.
  2. Adicionar `proxy_http_version 1.1;` e `proxy_set_header Connection "";` no bloco de proxy upstream.
  3. Mover `proxy_buffering off` e `proxy_read_timeout 86400s` exclusivamente para a rota específica de SSE (`/notificacoes/stream`).
- **Esforço:** P
- **Relacionados:** A3 (Concorrência e Tempo Real)

---

### [INFRA-12] [🔵 Baixo] Ausência de Graceful Shutdown Configurado no Spring Boot
- **Domínio:** A5
- **Tipo:** Fato
- **Local/Evidência:** Arquivo `src/main/resources/application.properties` (sem diretivas `server.shutdown`); propriedades de parada do unit:
  ```bash
  $ systemctl show equadras-backend -p TimeoutStopUSec
  TimeoutStopUSec=1min 30s
  ```
- **Problema:** O Spring Boot opera por padrão com shutdown imediato (`server.shutdown=immediate`). Quando o comando de deploy ou reinicialização (`sudo systemctl restart equadras-backend`) é emitido, a JVM encerra imediatamente as conexões HTTP em andamento, abortando transações e interrompendo streams SSE de forma brusca, em vez de rejeitar novas requisições e conceder uma janela para que as requisições em voo terminem.
- **Impacto:** **Disponibilidade e Experiência do Usuário.** Conexões de clientes recebem erros 502/Bad Gateway ou conexões resetadas durante qualquer ciclo de deploy ou reinicialização.
- **Recomendação:** Configurar em `application.properties`:
  ```properties
  server.shutdown=graceful
  spring.lifecycle.timeout-per-shutdown-phase=20s
  ```
- **Esforço:** P
- **Relacionados:** A3 (Concorrência e Tempo Real)

---

## 4. Sinais para Outros Domínios

- **Para A3 (Concorrência e Tempo Real):**
  - O Nginx aplica `proxy_buffering off` e `proxy_read_timeout 86400s` para toda e qualquer requisição da API (e não apenas para o stream SSE), o que aumenta a quantidade de sockets abertos e a latência de transferência de dados normais.
  - A ausência de `proxy_http_version 1.1` força o Nginx a falar HTTP/1.0 com o Tomcat embedded, eliminando o keepalive entre o proxy reverso e a JVM.
- **Para A4 (Segurança):**
  - O arquivo `/etc/default/equadras-backend` com permissão `644` permite leitura local de todas as variáveis de ambiente sensíveis (senha do PostgreSQL e token do Mercado Pago).
  - O Spring Boot está escutando na interface `*:8080` (`0.0.0.0:8080`) e não exclusivamente em `127.0.0.1:8080`.
  - A porta 80 e 443 do Nginx aceita conexões diretas de qualquer IP na internet, permitindo contornar o WAF da Cloudflare caso o atacante descubra o IP `137.131.163.62`.
  - A ausência da configuração `real_ip` no Nginx repassa o IP da Cloudflare para a aplicação, comprometendo a eficácia e a justiça dos filtros de rate limit por IP (`RateLimitFilter`).
- **Para A6 (Frontend):**
  - O processo de deploy na nuvem depende de um script `rebuild_frontend.sh` que faz `sed -i` em `src/api/apiClient.ts` antes de rodar `npm run build`, revelando que a URL da API estava hardcoded no código do frontend em vez de utilizar variáveis de ambiente Vite (`import.meta.env.VITE_API_URL` ou caminhos relativos).
- **Para A7 (Qualidade e Entrega):**
  - O deploy contínuo em `.github/workflows/ci.yml` executa compilação Maven com `-DskipTests=true` diretamente na máquina de produção, sem validação da integridade dos testes no momento da geração do artefato final deployado.

---

## 5. O Que Não Pôde Ser Verificado e Por Quê (❓)

- ❓ **Regras de Rede / NSG na Console Web da OCI:** Não é possível verificar através da sessão SSH na VM se a VCN (Virtual Cloud Network) e a Security List associada na Console da Oracle Cloud possuem filtros adicionais de IP para as portas 80 e 443, ou se estão totalmente abertas para a Internet (`0.0.0.0/0`). *(Motivo: Acesso restrito via SSH à VM; sem credenciais de API/CLI da OCI).*
- ❓ **Configuração e Retenção de Backups no Dashboard do Supabase:** Não foi possível inspecionar visualmente o painel do Supabase para confirmar se existe alguma rotina de exportação ou snapshot manual configurado fora da interface padrão do Free tier. *(Motivo: Acesso limitado ao banco de dados e à VM; sem acesso ao console web do provedor).*
- ❓ **Alertas de Ociosidade emitidos pela Oracle:** Não foi possível verificar se a conta associada à OCI já recebeu advertências por e-mail sobre ociosidade da VM nos últimos 7 dias. *(Motivo: Notificações de ociosidade são enviadas para a caixa de e-mail do proprietário da tenancy).*
