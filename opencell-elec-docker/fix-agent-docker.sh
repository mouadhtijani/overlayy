#!/bin/bash
set -e

JENKINS_USER=jenkins-agent  # adapte si besoin

echo "Vérification que le groupe docker existe..."
if ! getent group docker >/dev/null; then
  echo "Le groupe docker n'existe pas, création..."
  groupadd docker
fi

echo "Ajout de l'utilisateur $JENKINS_USER au groupe docker..."
if id "$JENKINS_USER" >/dev/null 2>&1; then
  usermod -aG docker $JENKINS_USER
  echo "Utilisateur $JENKINS_USER ajouté au groupe docker."
else
  echo "Utilisateur $JENKINS_USER introuvable sur cette machine."
  exit 1
fi

echo ""
echo "Groupes actuels de $JENKINS_USER :"
id $JENKINS_USER

echo ""
echo "⚠️ IMPORTANT : Pour que les changements prennent effet, il faudra redémarrer la session Jenkins (reboot ou relance conteneur)."

echo ""
echo "Voici le bloc YAML à ajouter ou vérifier dans ton fichier docker-compose.yml :"
echo "---------------------------------------------------------"
echo "services:"
echo "  jenkins:"
echo "    volumes:"
echo "      - /var/run/docker.sock:/var/run/docker.sock"
echo "      - jenkins_home:/var/jenkins_home"
echo "    group_add:"
echo "      - $(getent group docker | cut -d: -f3)"
echo ""
echo "volumes:"
echo "  jenkins_home:"
echo "---------------------------------------------------------"

echo ""
echo "Après avoir modifié docker-compose.yml, relance Jenkins avec :"
echo "  docker compose up -d jenkins"
