# Configuração da VM de produção

Arquivos de sistema da VM OCI (`137.131.163.62`, Ubuntu 24.04, 954 MiB de RAM) que ficam fora do deploy automático. O CI (`deploy/deploy.sh`) só troca o JAR e o frontend; estes arquivos são instalados manualmente e mudam raramente. Plano e diagnóstico: `docs/plans/vm-otimizacao-plano.md`.

Antes de alterar qualquer arquivo na VM, copie o original para `/opt/equadras/backups/infra-<data>/`.

| Arquivo | Destino na VM | Aplicar | Rollback |
|---|---|---|---|
| `equadras-backend.override.conf` | `/etc/systemd/system/equadras-backend.service.d/override.conf` | `sudo systemctl daemon-reload && sudo systemctl restart equadras-backend` (fica ~70 s fora do ar) | remover o diretório `.d`, `daemon-reload`, `restart` |
| `journald-equadras.conf` | `/etc/systemd/journald.conf.d/equadras.conf` | `sudo systemctl restart systemd-journald` | remover o arquivo e reiniciar o journald |
| `zramswap` | `/etc/default/zramswap` (pacote `zram-tools`) | `sudo systemctl restart zramswap` | `sudo systemctl disable --now zramswap` |
| `nginx/cloudflare-realip.conf` | `/etc/nginx/conf.d/cloudflare-realip.conf` | `sudo nginx -t && sudo systemctl reload nginx` | remover o arquivo, `nginx -t`, `reload` |
| `nginx/gzip.conf` | `/etc/nginx/conf.d/gzip.conf` | idem | idem |
| `nginx/equadras.conf` | `/etc/nginx/sites-available/equadras` | idem | restaurar o backup, `nginx -t`, `reload` |

Ao copiar a partir do Windows, converta as quebras de linha: `sed -i 's/\r$//' <arquivo>`.

## Outros ajustes feitos direto na VM

- `/etc/default/equadras-backend`: `HIKARI_MAX_POOL_SIZE=5` e `HIKARI_MIN_IDLE=1`.
- Serviços desativados e mascarados (sem uso nesta VM): `fwupd`, `fwupd-refresh.timer`, `packagekit`, `ModemManager`, `udisks2`, `multipathd` (+ socket). Para reverter: `sudo systemctl unmask <unit> && sudo systemctl enable --now <unit>`.

## Manutenção

- `nginx/cloudflare-realip.conf` lista as faixas de IP do Cloudflare com a data da consulta. Se o Cloudflare publicar faixas novas, o tráfego delas passa a aparecer com o IP do Cloudflare no lugar do IP do cliente; regere o arquivo a partir de https://www.cloudflare.com/ips-v4 e https://www.cloudflare.com/ips-v6.
- `nginx/equadras.conf` contém as linhas gerenciadas pelo Certbot; se o Certbot reescrever o site na VM, atualize este arquivo a partir da VM.
