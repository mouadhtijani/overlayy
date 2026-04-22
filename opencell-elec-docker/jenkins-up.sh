#!/bin/bash

# Variables
AGENT_USER="jenkins-agent"
AGENT_HOME="/home/$AGENT_USER"
SSH_DIR="$AGENT_HOME/.ssh"
WORKSPACE_DIR="$AGENT_HOME/workspace"
KEY_NAME="id_rsa_jenkins_agent"

# 1. Créer l'utilisateur agent s'il n'existe pas
if id "$AGENT_USER" &>/dev/null; then
    echo "[INFO] Utilisateur $AGENT_USER existe déjà."
else
    echo "[INFO] Création de l'utilisateur $AGENT_USER..."
    sudo adduser --disabled-password --gecos "" $AGENT_USER
fi

# 2. Créer le répertoire .ssh
echo "[INFO] Préparation du dossier .ssh..."
sudo mkdir -p $SSH_DIR
sudo chown $AGENT_USER:$AGENT_USER $SSH_DIR
sudo chmod 700 $SSH_DIR

# 3. Générer une nouvelle paire de clés SSH (sans mot de passe)
echo "[INFO] Génération de la clé SSH..."
sudo -u $AGENT_USER ssh-keygen -t rsa -b 4096 -f $SSH_DIR/$KEY_NAME -N ""

# 4. Ajouter la clé publique dans authorized_keys
echo "[INFO] Ajout de la clé publique à authorized_keys..."
sudo bash -c "cat $SSH_DIR/$KEY_NAME.pub >> $SSH_DIR/authorized_keys"
sudo chown $AGENT_USER:$AGENT_USER $SSH_DIR/authorized_keys
sudo chmod 600 $SSH_DIR/authorized_keys

# 5. Créer le répertoire workspace
echo "[INFO] Création du dossier de travail Jenkins..."
sudo mkdir -p $WORKSPACE_DIR
sudo chown $AGENT_USER:$AGENT_USER $WORKSPACE_DIR

# 6. Affichage de la clé privée à coller dans Jenkins
echo ""
echo "✅ CONFIGURATION TERMINÉE"
echo "📋 Clé privée à coller dans Jenkins (Manage Jenkins > Credentials > SSH):"
echo "-------------------------------------------------------------------"
sudo cat $SSH_DIR/$KEY_NAME
echo "-------------------------------------------------------------------"
echo "📎 Nom d'utilisateur : $AGENT_USER"
echo "📂 Répertoire de travail distant : $WORKSPACE_DIR"
echo "🧩 Hôte (à configurer dans Jenkins) : IP ou hostname de la VM"
echo ""
echo "default password is : "

docker exec -it jenkins cat /var/jenkins_home/secrets/initialAdminPassword

