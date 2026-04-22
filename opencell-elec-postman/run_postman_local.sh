#!/bin/bash

# Définition des variables (les fichiers doivent être dans le même répertoire que ce script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLLECTION="$SCRIPT_DIR/generated/epi-full-release.postman_collection.json"
ENVIRONMENT="$SCRIPT_DIR/env/localhost.postman_environment.json"
REPORT_FOLDER="$SCRIPT_DIR/reports"
HTML_REPORT="$REPORT_FOLDER/report.html"
JSON_REPORT="$REPORT_FOLDER/report.json"

# Créer le dossier de rapport s'il n'existe pas
mkdir -p "$REPORT_FOLDER"

# Vérifier si les modules nécessaires sont installés
if ! npm list -g | grep -q newman-reporter-html; then
    echo "Installing newman-reporter-html module..."
    npm install -g newman-reporter-html
fi

# Exécuter la collection avec rapports HTML et JSON
newman run "$COLLECTION" \
    -e "$ENVIRONMENT" \
    --reporters cli,html,json \
    --reporter-html-export "$HTML_REPORT" \
    --reporter-json-export "$JSON_REPORT"

# Vérification de la génération
if [ $? -eq 0 ]; then
    echo "✅ Rapport HTML généré : $HTML_REPORT"
    echo "✅ Rapport JSON généré : $JSON_REPORT"
else
    echo "❌ Une erreur est survenue lors de l'exécution."
fi
