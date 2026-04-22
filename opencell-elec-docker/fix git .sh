#!/bin/bash

JENKINS_USER=jenkins-agent
JENKINS_HOME=/home/$JENKINS_USER
SRC_SSH_DIR=/root/.ssh
DST_SSH_DIR=$JENKINS_HOME/.ssh

echo "Copie de la config SSH et des clés de $SRC_SSH_DIR vers $DST_SSH_DIR"

mkdir -p $DST_SSH_DIR

cp $SRC_SSH_DIR/config $DST_SSH_DIR/
cp $SRC_SSH_DIR/id_rsa_bitbucket1 $DST_SSH_DIR/
cp $SRC_SSH_DIR/id_rsa_bitbucket1.pub $DST_SSH_DIR/
cp $SRC_SSH_DIR/id_rsa_bitbucket2 $DST_SSH_DIR/
cp $SRC_SSH_DIR/id_rsa_bitbucket2.pub $DST_SSH_DIR/
cp $SRC_SSH_DIR/known_hosts $DST_SSH_DIR/

chown -R $JENKINS_USER:$JENKINS_USER $DST_SSH_DIR

chmod 700 $DST_SSH_DIR
chmod 600 $DST_SSH_DIR/id_rsa_bitbucket1
chmod 600 $DST_SSH_DIR/id_rsa_bitbucket2
chmod 644 $DST_SSH_DIR/id_rsa_bitbucket1.pub
chmod 644 $DST_SSH_DIR/id_rsa_bitbucket2.pub
chmod 644 $DST_SSH_DIR/known_hosts

echo "Modification du fichier config pour utiliser des chemins relatifs..."
sed -i 's|IdentityFile /root/.ssh|IdentityFile ~/.ssh|g' $DST_SSH_DIR/config

echo "Copie terminée, permissions ajustées."

echo "Teste la connexion SSH en tant que $JENKINS_USER :"
sudo -u $JENKINS_USER ssh -T core
sudo -u $JENKINS_USER ssh -T overlay
