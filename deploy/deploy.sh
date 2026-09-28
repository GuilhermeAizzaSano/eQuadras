#!/usr/bin/env bash
# Ativa uma release já enviada pelo CI para /opt/equadras/releases/<versao>.
# Troca o symlink "current", reinicia o backend e valida a subida via health check.
# Se a nova versão não responder, restaura a release anterior automaticamente.
#
# Uso: deploy.sh <versao>
set -euo pipefail

BASE_DIR=/opt/equadras
RELEASES_DIR="$BASE_DIR/releases"
CURRENT_LINK="$BASE_DIR/current"
SERVICO=equadras-backend.service
HEALTH_URL=http://127.0.0.1:8080/quadras
HEALTH_TIMEOUT_S=180
RELEASES_MANTIDAS=5

VERSAO="${1:?Informe a versão (sha do commit) a ativar}"
NOVA_RELEASE="$RELEASES_DIR/$VERSAO"

if [ ! -f "$NOVA_RELEASE/equadras.jar" ] || [ ! -f "$NOVA_RELEASE/frontend/index.html" ]; then
  echo "ERRO: release incompleta em $NOVA_RELEASE" >&2
  exit 1
fi

RELEASE_ANTERIOR=""
if [ -L "$CURRENT_LINK" ]; then
  RELEASE_ANTERIOR="$(readlink -f "$CURRENT_LINK")"
fi

apontar_current() {
  # ln + mv garante troca atômica do symlink
  ln -sfn "$1" "$CURRENT_LINK.tmp"
  mv -Tf "$CURRENT_LINK.tmp" "$CURRENT_LINK"
}

aguardar_backend() {
  local limite=$((SECONDS + HEALTH_TIMEOUT_S))
  local codigo
  while [ "$SECONDS" -lt "$limite" ]; do
    # Qualquer resposta HTTP abaixo de 500 (inclusive 401 em rota protegida) prova que a aplicação subiu
    codigo="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$HEALTH_URL" || true)"
    if [ "$codigo" != "000" ] && [ "$codigo" -lt 500 ]; then
      echo "==> Backend respondeu HTTP $codigo"
      return 0
    fi
    sleep 5
  done
  return 1
}

echo "==> Ativando release $VERSAO (anterior: ${RELEASE_ANTERIOR:-nenhuma})"
apontar_current "$NOVA_RELEASE"
sudo systemctl restart "$SERVICO"

if aguardar_backend; then
  sudo systemctl reload nginx
  echo "==> Release $VERSAO ativa."
else
  echo "ERRO: backend não respondeu em ${HEALTH_TIMEOUT_S}s." >&2
  sudo journalctl -u "$SERVICO" -n 40 --no-pager >&2 || true
  if [ -n "$RELEASE_ANTERIOR" ] && [ "$RELEASE_ANTERIOR" != "$NOVA_RELEASE" ]; then
    echo "==> Rollback para $(basename "$RELEASE_ANTERIOR")" >&2
    apontar_current "$RELEASE_ANTERIOR"
    sudo systemctl restart "$SERVICO"
    if aguardar_backend; then
      sudo systemctl reload nginx
      echo "==> Rollback concluído; versão anterior restaurada." >&2
    else
      echo "ERRO CRÍTICO: rollback também não respondeu. Intervenção manual necessária." >&2
    fi
  fi
  exit 1
fi

# Mantém apenas as releases mais recentes, preservando sempre a atual
ATUAL="$(readlink -f "$CURRENT_LINK")"
ls -1dt "$RELEASES_DIR"/*/ | sed 's:/$::' | tail -n +$((RELEASES_MANTIDAS + 1)) | while read -r antiga; do
  [ "$antiga" = "$ATUAL" ] || rm -rf "$antiga"
done
