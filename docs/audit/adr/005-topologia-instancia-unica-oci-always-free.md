# ADR 005: Implantação em Instância Única (SPOF) na OCI Always Free

- **Status:** Proposto — retroativo
- **Data:** 2026-09-23
- **Decisores:** Time de Desenvolvimento eQuadras
- **Domínios Afetados:** A4 (Security), A5 (Infra & Operations), A7 (Quality & Delivery)

---

## 1. Contexto

O projeto eQuadras iniciou como um sistema universitário/acadêmico com restrição de custo zero de infraestrutura. A Oracle Cloud Infrastructure (OCI) disponibiliza instâncias computacionais gratuitas perpétuas (Always Free).

## 2. Decisão

Implantou-se toda a camada de execução (Nginx reverse proxy + Spring Boot JAR sob systemd + uploads locais em disco) em uma única VM `VM.Standard.E2.1.micro` (1 OCPU AMD EPYC, 1.0 GB RAM, 50 GB NVMe) em São Paulo (`sa-saopaulo-1`).

## 3. Consequências e Riscos Identificados

### Positivas
- **Custo Zero:** Enquadra-se integralmente na gratuidade da OCI.
- **Topologia Simples:** Nginx local faz proxy direto para `127.0.0.1:8080`, sem necessidade de malha de rede, balanceadores externos ou orquestradores (Kubernetes).

### Negativas / Riscos Revelados na Auditoria
- **Ponto Único de Falha (SPOF Total - A5):** A queda ou travamento da VM derruba 100% da plataforma. Não há redundância nem failover automatizado. O RTO real estimado (2 a 4+ horas) viola a meta de ≤ 2 horas caso haja corrupção do disco ou desprovisionamento.
- **Gargalo Extremo de Memória (1 GB RAM - A5):** O SO Ubuntu + Nginx + JVM (heap max 320 MB) operam próximos ao limite (swap ativo em ~340 MB). A rotina de CI/CD que executa `./mvnw clean package` na própria VM durante o deploy derruba o sistema por Out-of-Memory (OOM Killer).
- **Perda de Sessões SSE em Deploy (A3/A5):** Cada deploy exige reinicialização do systemd (`systemctl restart equadras-backend`), derrubando abruptamente todas as conexões SSE ativas.
- **Exposição de Borda (A4/A5):** As portas 80 e 443 da VM estão abertas para `0.0.0.0/0` no firewall iptables, permitindo acesso direto ao IP da VM e contorno das proteções WAF e DDoS da Cloudflare.
- **Armazenamento de Imagens em Disco Local (A1/A4/A5):** Imagens de quadras são salvas na pasta local `/home/ubuntu/eQuadras/uploads`, impedindo escala horizontal e demandando backup manual do disco da VM.
