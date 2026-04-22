# Documentation de Projet

## Installation des Outils

Avant de commencer, assurez-vous d'avoir les outils suivants installés sur votre système :

- Docker
- Apache Maven (Maven)
- JDK 11 (Java Development Kit)
- IntelliJ IDEA (ou un autre IDE de votre choix)
- Git Bash
- Postman

## Construction du Projet

1. Ouvrez un terminal ou une ligne de commande.

2. Naviguez jusqu'au répertoire "core" d'OpenCell :

3. Exécutez la commande pour nettoyer et construire le projet OpenCell en version 14.2.0 :
```powershell
mvn clean install
```
4. Naviguez jusqu'au répertoire principal du projet :

5. Exécutez la commande pour nettoyer et construire le projet overlay  :
```powershell
mvn clean install
```

## Utilisation de Docker Compose depuis opencell-elec-docker

le Docker Compose est présent dans le module `opencell-elec-docker`

1. Ouvrez un terminal ou une ligne de commande.

2. Naviguez jusqu'au répertoire du module `opencell-elec-docker` où se trouve le fichier Docker Compose :

```powershell
cd opencell-elec-docker
   ```
3. Telecharger l'artifact de OPENCELL V14.2.0 : 
   * Lancer dans git bash la commande suivante :
```SH
curl -L http://dl.opencellsoft.com/14.2.0/opencell.war -o input-files/opencell.war
   ```

4. Exécutez la commande suivante pour démarrer tous les conteneurs définis dans le fichier Docker Compose :

```powershell
docker-compose up -d
   ```

## Actions Manuelles dans PowerShell

Assurez-vous que Docker est en cours d'exécution avant de commencer ces étapes manuelles.

1. Redémarrez le conteneur PostgreSQL:

```powershell
docker restart postgres-wildfly
```

2. Attendez que le conteneur soit ouvert.

3. Exécutez les commandes suivantes pour accéder à PostgreSQL et créer un utilisateur et une base de données :

Lorsqu'on vous demande un mot de passe, entrez : opencell_db_password

```powershell
docker exec -it postgres-wildfly /bin/bash
psql -U opencell_db_user -d opencell_db -h localhost -W
CREATE USER keycloak WITH PASSWORD 'keycloak';
CREATE DATABASE keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
ALTER USER keycloak WITH SUPERUSER;
```

4. Puis quittez l'interface PostgreSQL :

```powershell
\q
exit
```

5. Redémarrez le conteneur Keycloak :

```powershell
docker restart keycloak
```
6. Deployer le portail rouge
   1. Accéder au conteneur Docker opencell
    ```powershell
    docker exec -ti opencell bash
    ```
   2. Naviguez jusqu'au répertoire /portal
    ```powershell
    cd opencelldata/default/frontend/portal/
    ```
    2. Modifier le fichier app-properties.js
    ```powershell
    vi app-properties.js
    ```
   3. Entrer dans le Mode edition en cliquant sur la bouton **i** ou **Inser** 
   4. Mettre a jour la valeur du  **KEYCLOAK_APP_AUTH_URL** :
   ```powershell
    KEYCLOAK_APP_AUTH_URL: 'http://localhost:9090/auth',
    ```
   5. Quitter le mode edtion en cliquant sur la bouton **Echap**
   6. Pour sauvgarder et quitter : 
    ```powershell
    :x
    exit
    ```
7. Redémarrez le conteneur opencell :

```powershell
docker restart opencell
```

## Accès aux Portails

Une fois que tous les conteneurs sont opérationnels, vous pourrez accéder aux portails via les liens suivants :

- Le Portail Bleu est accessible à l'adresse : [Lien vers le Portail Bleu](http://localhost:8080/opencell/)
- Le Portail Rouge est accessible à l'adresse : [Lien vers le Portail Rouge](http://localhost:8080/opencell/frontend/default/portal/index.html)
