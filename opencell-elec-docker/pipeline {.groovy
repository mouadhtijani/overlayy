pipeline {
    agent { label 'vagrant-ssh-agent' }

    parameters {
        booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: false, description: 'Clean workspace at the end')
        string(name: 'CORE_VERSION', defaultValue: 'release/14.3.6', description: 'Version complète (ex: release/14.3.6)')
        choice(name: 'DEPLOY_MODE', choices: ['init_env','last_release'], description: 'Mode de déploiement')
        choice(name: 'RELEASE_TYPE', choices: ['Majeur', 'mineur', 'patch'], description: 'Type de release à déployer')
    }

    stages {
        
        stage('Clone repo core') {
            steps {
                script {
                    echo "Cloning repository core with branch ${params.CORE_VERSION}..."
                    dir('core') {
                        git url: 'git@core:opencellsoft/opencell-core.git', branch: params.CORE_VERSION
                    }
                }
            }
        }

        stage('Build core with Maven') {
            steps {
                dir('core') {
                    echo "Building core with Maven, skipping tests..."
                    sh 'mvn clean install -Dmaven.test.skip=true -Dmaven.repo.local=$WORKSPACE/.m2/repository'
                }
            }
        }

        stage('Clone repo overlay') {
            steps {
                echo 'Cloning repository overlay...'
                dir('overlay') {
                    git url: 'git@overlay:testiliade/epi_overlay.git', branch: 'master'
                }
            }
        }

        stage('Update inputfiles in overlay') {
            steps {
                script {
                    def fullVersion = params.CORE_VERSION
                    def version = fullVersion.tokenize('/').last()
                    echo "Using version number extracted: ${version}"

                    def baseUrl = "http://dl.opencellsoft.com/${version}"

                    dir('overlay/opencell-elec-docker/input-files') {
                        echo "Cleaning targeted files in inputfiles folder..."
                        def files = ['import-postgres.sql', 'opencell-realm-and-users.json']
                        
                        files.each { file ->
                            sh "rm -f ${file} || true"
                        }

                        echo "Downloading required files from ${baseUrl} ..."
                        files.each { file ->
                            sh "curl -f -O ${baseUrl}/${file}"
                        }
                    }
                }
            }
        }


        stage('Update versions in overlay') {
            steps {
                script {
                    def fullVersion = params.CORE_VERSION
                    def version = fullVersion.tokenize('/').last()
                    echo "Using version number extracted: ${version}"

                    dir('overlay') {
                       sh """
                        sed -i 's|<opencell.version>.*</opencell.version>|<opencell.version>${version}</opencell.version>|' pom.xml

                        sed -i 's|^\\(\\s*OPENCELL_VERSION:\\s*\\).*|\\1${version}|' opencell-elec-docker/docker-compose.yml
                        
                        sed -i 's|opencell-model-[0-9.]*\\.jar|opencell-model-${version}.jar|' opencell-elec-overlay/src/main/resources/META-INF/persistence.xml
                        """
                    }
                }
            }
        }

        stage('Build overlay with Maven') {
            steps {
                dir('overlay') {
                    echo "Building overlay with Maven, skipping tests..."
                    sh 'mvn clean install -Dmaven.test.skip=true -Dmaven.repo.local=$WORKSPACE/.m2/repository'
                }
            }
        }



        stage('Docker compose down') {
            steps {
                script {
                    dir('overlay/opencell-elec-docker') {
                        if (params.DEPLOY_MODE == 'init_env') {
                            echo "Docker compose down with volumes removal (-v) for init_env"
                            sh 'docker compose down -v || true'
                        } else {
                            echo "Docker compose down without volumes removal for last release"
                            sh 'docker compose down || true'
                        }
                    }
                }
            }
        }    


        stage('Docker compose up') {
            steps {
                dir('overlay/opencell-elec-docker') {
                    echo 'Launching docker compose up -d ...'
                    sh 'docker compose up -d'
                }
            }
        }        
        stage('Wait deploy overlay') {
            steps {
                echo 'Pause automatique...'
                sh 'sleep 360'
            }
        }
        stage('DEPLOY ALL SCRIPTS') {
            steps {
                dir('overlay/opencell-elec-script/src/generated') {
                    sh 'chmod +x deploy_scripts.sh'
                    sh './deploy_scripts.sh'
                }
            }
        }

        stage('DEPLOY ALL PARAM') {
            when { expression { params.DEPLOY_MODE.startsWith('init_env')} }
            steps {
                dir('overlay/opencell-elec-postman/generated') {
                    sh 'chmod +x deploy_full_release.sh'
                    sh './deploy_full_release.sh'
                }
            }
        }
        stage('DEPLOY SQL FULL RELEASE') {
            when { expression { params.DEPLOY_MODE.startsWith('init_env') } }
            steps {
                script {
                    dir('overlay/opencell-elec-database/FullDeploy') {
                        echo "Exécution directe de deploy.sql dans postgres-wildfly avec mot de passe"

                        sh '''
                            docker exec -i postgres-wildfly sh -c "PGPASSWORD=opencell_db_password psql -U opencell_db_user -d opencell_db" < deploy.sql
                        '''
                    }
                }
            }
        }

        stage('PREPARE LAST RELEASE') {
            when { expression { params.DEPLOY_MODE.startsWith('last_release') } }
            steps {
                script {
                    def arg = ''
                    switch(params.RELEASE_TYPE) {
                        case 'Majeur':
                            arg = 'M'
                            break
                        case 'mineur':
                            arg = 'm'
                            break
                        case 'patch':
                            arg = 'p'
                            break
                        default:
                            error("Type de release inconnu : ${params.RELEASE_TYPE}")
                    }

                    dir('overlay/sh') {
                        sh 'chmod +x release.sh'
                        sh "./release.sh ${arg}"
                    }
                }
            }
        }

       stage('PRE_DEPLOY SQL') {
            when { 
                expression { params.DEPLOY_MODE.startsWith('last_release') } 
            }
            steps {
                script {
                    def sqlFile = 'overlay/opencell-elec-database/PostDeploy/generated/PreDeploy-full.sql'
                    if (fileExists(sqlFile)) {
                        dir('overlay/opencell-elec-database/PostDeploy/generated') {
                            echo "Exécution directe de PreDeploy-full.sql dans postgres-wildfly avec mot de passe"
                            sh '''
                                docker exec -i postgres-wildfly sh -c "PGPASSWORD=opencell_db_password psql -U opencell_db_user -d opencell_db" < PreDeploy-full.sql
                            '''
                        }
                    } else {
                        echo "Fichier PreDeploy-full.sql non trouvé. Stage sauté avec succès."
                    }
                }
            }
        }



        stage('DEPLOY LAST RELEASE PARAM') {
            when { expression { params.DEPLOY_MODE.startsWith('last_release') } }
            steps {
                script {
                    def scriptFile = 'overlay/opencell-elec-postman/generated/deploy_last_release.sh'
                    if (fileExists(scriptFile)) {
                        dir('overlay/opencell-elec-postman/generated') {
                            sh 'chmod +x deploy_last_release.sh'
                            sh './deploy_last_release.sh'
                        }
                    } else {
                        echo "Script deploy_last_release.sh non trouvé. Stage sauté avec succès."
                    }
                }
            }
        }
       stage('POST_DEPLOY SQL') {
            when { 
                expression { params.DEPLOY_MODE.startsWith('last_release') }
            }
            steps {
                script {
                    def sqlFile = 'overlay/opencell-elec-database/PostDeploy/generated/PostDeploy-full.sql'
                    if (fileExists(sqlFile)) {
                        dir('overlay/opencell-elec-database/PostDeploy/generated') {
                            echo "Exécution directe de PostDeploy-full.sql dans postgres-wildfly avec mot de passe"
                            sh '''
                                docker exec -i postgres-wildfly sh -c "PGPASSWORD=opencell_db_password psql -U opencell_db_user -d opencell_db" < PostDeploy-full.sql
                            '''
                        }
                    } else {
                        echo "Fichier PostDeploy-full.sql non trouvé. Stage sauté avec succès."
                    }
                }
            }
        }

        stage('Enable portal') {
            steps {
                script {
                    sh """
                    docker exec opencell sed -i "s|KEYCLOAK_APP_AUTH_URL: '\\/auth',|KEYCLOAK_APP_AUTH_URL: 'http://192.168.56.50:9090/auth',|" opencelldata/default/frontend/portal/app-properties.js
                    """
                }
            }
        }    
        stage('DEPLOY ALL Roles') {
            when { expression { params.DEPLOY_MODE.startsWith('init_env')} }
            steps {
                dir('overlay/opencell-elec-postman/generated') {
                    sh 'chmod +x deploy_roles.sh'
                    sh './deploy_roles.sh'
                }
            }
        }

    }

    post {
        always {
            script {
                if (params.CLEAN_WORKSPACE) {
                    echo 'Cleaning workspace (post block)...'
                    deleteDir()
                } else {
                    echo 'Skipping workspace clean (post block)...'
                }
            }
        }
    }
}
