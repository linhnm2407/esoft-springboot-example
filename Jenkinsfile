pipeline {
    agent any 
    tools {
        jdk 'jdk17'
        maven 'maven3'
    }
    environment  {
        SCANNER_HOME=tool 'sonar-scanner'
        AWS_ACCOUNT_ID = credentials('ACCOUNT_ID')
        AWS_ECR_REPO_NAME = credentials('ECR_REPO')
        AWS_DEFAULT_REGION = 'ap-southeast-2'
        REPOSITORY_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/"
        TOKEN = credentials('GITHUB_PAN')
    }
    stages {
        stage('Cleaning Workspace') {
            steps {
                cleanWs()
            }
        }
        stage('Checkout from Git') {
            steps {
                git branch: 'staging' credentialsId: 'GITHUB_ACCOUNT', url: 'https://github.com/linhnm2407/esoft-springboot-example.git'
            }
        }
        stage('mvn Compile'){
            steps{
                sh 'mvn clean compile'
            }
        }
        stage('mvn Test'){
            steps{
                sh 'mvn test'
            }
        }
        stage('Sonarqube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                        sh ''' 
                        mvn clean verify sonar:sonar \
                        -Dsonar.projectKey=esoft-springboot-example-stag \
                        '''
                    }                
            }
        }
        stage('Quality Check') {
            steps {
                script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'sonar-token' 
                }
            }
        }
        stage('mvn Build') {
            steps {
                sh 'mvn clean install'
            }
        }
        stage('OWASP Dependency-Check Scan') {
            steps {
                dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage('Trivy File Scan') {
            steps {
                sh 'trivy fs . > trivyfs.txt'
            }
        }
        stage("Docker Image Build") {
            steps {
                script {
                    sh 'docker system prune -f'
                    sh 'docker container prune -f'
                    sh 'docker build -t ${AWS_ECR_REPO_NAME} .'
                }
            }
        }
        stage("ECR Image Pushing") {
            steps {
                script {
                        sh 'aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${REPOSITORY_URI}'
                        sh 'docker tag ${AWS_ECR_REPO_NAME} ${REPOSITORY_URI}${AWS_ECR_REPO_NAME}:${BUILD_NUMBER}'
                        sh 'docker push ${REPOSITORY_URI}${AWS_ECR_REPO_NAME}:${BUILD_NUMBER}'
                }
            }
        }
        stage("TRIVY Image Scan") {
            steps {
                sh 'trivy image ${REPOSITORY_URI}${AWS_ECR_REPO_NAME}:${BUILD_NUMBER} > trivyimage.txt' 
            }
        }
        stage('Checkout Code') {
            steps {
                git credentialsId: 'GITHUB_ACCOUNT', url: 'https://github.com/linhnm2407/esoft-test-deploy.git'
            }
        }
        stage('Update Deployment file') {
            environment {
                GIT_REPO_NAME = "esoft-test-deploy"
                GIT_USER_NAME = "linhnm2407"
            }
            steps {
                dir('chart') {
                    withCredentials([string(credentialsId: 'GITHUB_PAN', variable: 'TOKEN')]) {
                        sh '''
                            git config user.email "linhnm2407@gmail.com"
                            git config user.name "linhnm2407"
                            BUILD_NUMBER=${BUILD_NUMBER}
                            echo $BUILD_NUMBER
                            imageTag=$(grep -oP '(?<=esoft-springboot:)[^ ]+' values-stag.yaml)
                            echo $imageTag
                            sed -i "s/${AWS_ECR_REPO_NAME}:${imageTag}/${AWS_ECR_REPO_NAME}:${BUILD_NUMBER}/" values-stag.yaml
                            git add values-stag.yaml
                            git commit -m "Update deployment Image to version \${BUILD_NUMBER}"
                            git push https://${TOKEN}@github.com/${GIT_USER_NAME}/${GIT_REPO_NAME} HEAD:master
                        '''
                    }
                }                
            }
        }
    }
}