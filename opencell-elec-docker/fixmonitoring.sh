#!/bin/bash

set -e

# === Fix PROMETHEUS ===
PROMETHEUS_DIR="/home/vagrant/prometheus"
PROMETHEUS_FILE="$PROMETHEUS_DIR/prometheus.yml"

mkdir -p "$PROMETHEUS_DIR"

if [ -d "$PROMETHEUS_FILE" ]; then
    rm -rf "$PROMETHEUS_FILE"
fi

if [ ! -f "$PROMETHEUS_FILE" ] || [ ! -s "$PROMETHEUS_FILE" ]; then
    cat > "$PROMETHEUS_FILE" <<EOF
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF
    echo "[OK] prometheus.yml corrigé"
else
    echo "[OK] prometheus.yml existe déjà"
fi

# === Fix PROMTAIL ===
PROMTAIL_FILE="/home/vagrant/promtail-config.yaml"

if [ -d "$PROMTAIL_FILE" ]; then
    rm -rf "$PROMTAIL_FILE"
fi

if [ ! -f "$PROMTAIL_FILE" ] || [ ! -s "$PROMTAIL_FILE" ]; then
    cat > "$PROMTAIL_FILE" <<EOF
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: system
    static_configs:
      - targets:
          - localhost
        labels:
          job: varlogs
          __path__: /var/log/*log
EOF
    echo "[OK] promtail-config.yaml corrigé"
else
    echo "[OK] promtail-config.yaml existe déjà"
fi

# === Fix LOKI ===
LOKI_FILE="/home/vagrant/loki-config.yaml"

if [ -d "$LOKI_FILE" ]; then
    rm -rf "$LOKI_FILE"
fi

if [ ! -f "$LOKI_FILE" ] || [ ! -s "$LOKI_FILE" ]; then
    cat > "$LOKI_FILE" <<EOF
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
    final_sleep: 0s
  chunk_idle_period: 5m
  chunk_retain_period: 30s
  max_transfer_retries: 0

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/index
    cache_location: /loki/index_cache
    shared_store: filesystem
  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 0s

table_manager:
  retention_deletes_enabled: false
  retention_period: 0s
EOF
    echo "[OK] loki-config.yaml corrigé"
else
    echo "[OK] loki-config.yaml existe déjà"
fi

# === Résumé ===
echo
ls -l "$PROMETHEUS_FILE"
ls -l "$PROMTAIL_FILE"
ls -l "$LOKI_FILE"
echo -e "\nTu peux relancer : docker-compose up -d"

