#!/bin/bash

set -e

LOKI_BASE="/home/vagrant/loki"

echo "Création des dossiers Loki nécessaires..."

mkdir -p "$LOKI_BASE/chunks"
mkdir -p "$LOKI_BASE/index"
mkdir -p "$LOKI_BASE/index_cache"
mkdir -p "$LOKI_BASE/wal"

chmod -R 777 "$LOKI_BASE"

echo "[OK] Dossiers Loki créés et permissions appliquées :"
ls -ld "$LOKI_BASE"/*
