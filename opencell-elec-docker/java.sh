
#!/bin/bash

echo "🔍 Vérification des versions Java installées..."
java -version

echo "🔄 Installation de Java 17 (openjdk-17-jdk)..."
sudo apt update
sudo apt install -y openjdk-17-jdk

echo "✅ Java 17 installé avec succès."

echo "🔍 Liste des versions Java disponibles via update-alternatives..."
JAVA17_PATH=$(update-alternatives --list java | grep "java-17")

if [ -z "$JAVA17_PATH" ]; then
    echo "❌ Java 17 non trouvé dans update-alternatives."
    exit 1
fi

echo ""
echo "📍 Chemin Java 17 trouvé : $JAVA17_PATH"

echo ""
echo "📋 Copie ce chemin dans Jenkins > Nodes > vagrant-agent > Configure"
echo "Champ : Java path on remote"
echo ""
echo "➡️ Java path on remote : $JAVA17_PATH"
echo ""
echo "⚠️ Ne change PAS la version Java par défaut pour le système."
echo "Ton projet continuera à utiliser Java 11 par défaut (/usr/bin/java)."
