#!/bin/bash

# Répertoire base de Loki (adapte-le si nécessaire)
LOKI_BASE="/home/vagrant/loki"

# Création des dossiers requis
mkdir -p "$LOKI_BASE/chunks"
mkdir -p "$LOKI_BASE/index"
mkdir -p "$LOKI_BASE/index_cache"
mkdir -p "$LOKI_BASE/wal"
mkdir -p "$LOKI_BASE/compactor"

# Application des droits pour éviter les soucis de permissions
chmod -R 777 "$LOKI_BASE"

echo "[OK] Tous les dossiers Loki créés et permissions appliquées :"
ls -ld "$LOKI_BASE"/*
