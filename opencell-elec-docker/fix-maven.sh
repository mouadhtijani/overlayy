#!/bin/bash

# Chemin absolu du dossier Maven
MAVEN_CONF_DIR="/opt/apache-maven-3.9.6/conf"

# Fichier source settings.xml à copier (dans le même dossier que le script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_SETTINGS_FILE="$SCRIPT_DIR/settings.xml"

# Vérification que le fichier source existe
if [ ! -f "$SRC_SETTINGS_FILE" ]; then
  echo "Erreur : Le fichier settings.xml n'existe pas dans $SCRIPT_DIR"
  exit 1
fi

# Copie du fichier settings.xml vers le dossier de configuration Maven
cp -f "$SRC_SETTINGS_FILE" "$MAVEN_CONF_DIR/settings.xml"

# Vérification du succès
if [ $? -eq 0 ]; then
  echo "Le fichier settings.xml a été remplacé avec succès dans $MAVEN_CONF_DIR"
else
  echo "Erreur lors de la copie du fichier settings.xml"
  exit 1
fi
