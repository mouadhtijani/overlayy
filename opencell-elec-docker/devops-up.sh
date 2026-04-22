#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

echo "🔧 Démarrage de l'installation DevOps..."

# --- JAVA 11 ---
echo "🔍 Vérification de Java..."
if ! java -version 2>&1 | grep -q "11"; then
  echo "➡️ Installation de Java 11..."
  sudo apt update
  sudo apt install -y openjdk-11-jdk
else
  echo "✅ Java 11 déjà installé"
fi

# --- MAVEN ---
echo "🔍 Vérification de Maven..."
if ! command -v mvn &> /dev/null; then
  echo "➡️ Installation de Maven 3.9.6..."
  MAVEN_VERSION=3.9.6
  cd /tmp
  curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o maven.tar.gz
  sudo tar -xzf maven.tar.gz -C /opt
  sudo ln -sfn /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn
  echo "✅ Maven ${MAVEN_VERSION} installé"
else
  echo "✅ Maven déjà installé"
fi

# --- DOCKER ---
echo "🔍 Vérification de Docker..."
if ! command -v docker &> /dev/null; then
  echo "➡️ Installation de Docker..."
  sudo apt install -y ca-certificates curl gnupg lsb-release
  sudo mkdir -p /etc/apt/keyrings
  if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  fi
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt update
  sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo systemctl enable docker
  sudo systemctl start docker
  sudo usermod -aG docker "$USER"
  echo "✅ Docker installé et activé"
else
  echo "✅ Docker déjà installé"
fi

# --- GENERATION DES CLES SSH ---
echo "🔑 Génération de deux clés SSH RSA 4096 bits pour Bitbucket..."

SSH_DIR="$HOME/.ssh"
KEY1="$SSH_DIR/id_rsa_bitbucket1"
KEY2="$SSH_DIR/id_rsa_bitbucket2"

mkdir -p "$SSH_DIR"
chmod 700 "$SSH_DIR"

generate_key() {
  local key_path=$1
  if [ ! -f "${key_path}" ]; then
    ssh-keygen -t rsa -b 4096 -f "${key_path}" -N "" -C "bitbucket-key-$(basename ${key_path})"
    echo "✅ Clé SSH générée : ${key_path}"
  else
    echo "✅ Clé SSH existe déjà : ${key_path}"
  fi
}

generate_key "$KEY1"
generate_key "$KEY2"

# --- CREATION DU FICHIER CONFIG SSH ---
CONFIG_FILE="$SSH_DIR/config"

echo "📝 Configuration du fichier SSH config..."

cat > "$CONFIG_FILE" <<EOF
# Configuration Bitbucket clé 1
Host core
  HostName bitbucket.org
  User git
  IdentityFile $KEY1
  IdentitiesOnly yes

# Configuration Bitbucket clé 2
Host overlay
  HostName bitbucket.org
  User git
  IdentityFile $KEY2
  IdentitiesOnly yes
EOF

chmod 600 "$CONFIG_FILE"

echo "✅ Fichier SSH config créé ou mis à jour : $CONFIG_FILE"

# Affichage des clés publiques
echo -e "\n🔍 Clés publiques SSH à ajouter sur Bitbucket :"

echo -e "\n--- Clé publique 1 (core) ---"
cat "${KEY1}.pub"

echo -e "\n--- Clé publique 2 (overlay) ---"
cat "${KEY2}.pub"

# --- DOCKER COMPOSE FILE ---
if [ ! -f "$COMPOSE_FILE" ]; then
  echo "📦 Création de $COMPOSE_FILE avec Jenkins, Nexus et Adminer..."

  cat <<EOF > "$COMPOSE_FILE"
version: "3.9"
services:

  jenkins:
    image: jenkins/jenkins:lts
    container_name: jenkins
    ports:
      - "50000:50000"
      - "9091:8080"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    group_add:
      - 999
    restart: always

  nexus:
    image: sonatype/nexus3
    container_name: nexus
    ports:
      - "8081:8081"
      - "8085:8085"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - nexus_data:/nexus-data
    group_add:
      - 999
    restart: always

  adminer:
    image: adminer
    container_name: adminer
    ports:
      - "8082:8080"
    restart: always

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9050:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    restart: always

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
    restart: always

  loki:
    image: grafana/loki:2.8.2
    container_name: loki
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yaml:/etc/loki/local-config.yaml
      - ./loki/chunks:/loki/chunks
      - ./loki/index:/loki/index
      - ./loki/index_cache:/loki/index_cache
      - ./loki/wal:/wal
      - ./loki/compactor:/loki/compactor
    restart: always

  promtail:
    image: grafana/promtail:2.8.2
    container_name: promtail
    volumes:
      - ./promtail-config.yaml:/etc/promtail/promtail-config.yaml
      - /var/log:/var/log
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /etc/machine-id:/etc/machine-id:ro
      - /var/run/docker.sock:/var/run/docker.sock
    command: -config.file=/etc/promtail/promtail-config.yaml
    restart: always

  node-exporter:
    image: prom/node-exporter:latest
    container_name: node-exporter
    restart: always
    network_mode: host
    pid: host
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
  sonarqube:
    image: sonarqube:community
    container_name: sonarqube
    ports:
      - "9000:9000"
    environment:
      - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_logs:/opt/sonarqube/logs

  volumes:
    sonarqube_data:
    sonarqube_logs:



volumes:
  jenkins_home:
  nexus_data:
  grafana_data:

EOF

  echo "✅ Fichier $COMPOSE_FILE créé"
else
  echo "✅ Fichier docker-compose.yml déjà présent à $COMPOSE_FILE"
fi

# --- LANCEMENT COMPOSE ---
echo "🚀 Vérification des conteneurs existants..."
if docker ps --format '{{.Names}}' | grep -Eq 'jenkins|nexus|adminer'; then
  echo "✅ Les conteneurs sont déjà en cours d'exécution. Aucune action."
else
  echo "🚀 Lancement de Docker Compose..."
  docker compose -f "$COMPOSE_FILE" up -d
fi

echo "🎉 Installation DevOps terminée avec succès !"
