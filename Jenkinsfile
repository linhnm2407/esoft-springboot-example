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
        // REPOSITORY_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/"
        REPOSITORY_URI = "058264552037.dkr.ecr.ap-southeast-2.amazonaws.com/esoft-springboot"
    }
    stages {
        stage('Cleaning Workspace') {
            steps {
                cleanWs()
            }
        }
        stage('Checkout from Git') {
            steps {
                git credentialsId: 'GITHUB_ACCOUNT', url: 'https://github.com/linhnm2407/esoft-springboot-example.git'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }
//         stage('Sonarqube Analysis') {
//             steps {
//                 withSonarQubeEnv('sonar-server') {
//                         sh ''' 
//                         mvn clean verify sonar:sonar \
//   -Dsonar.projectKey=esoft-springboot-example \
//   -Dsonar.host.url=http://3.25.126.235:9000 \
//   -Dsonar.login=squ_1b7a2eea963f3f1fad058789641258582d5ce600
//                         '''
//                     }                
//             }
//         }
//         stage('Quality Check') {
//             steps {
//                 script {
//                     waitForQualityGate abortPipeline: false, credentialsId: 'sonar-token' 
//                 }
//             }
//         }
        // stage('OWASP Dependency-Check Scan') {
        //     steps {
        //         dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
        //         dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
        //     }
        // }
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
                        sh 'aws ecr get-login-password --region ap-southeast-2 | docker login --username AWS --password-stdin 058264552037.dkr.ecr.ap-southeast-2.amazonaws.com'
                        sh 'docker tag esoft-springboot:latest 058264552037.dkr.ecr.ap-southeast-2.amazonaws.com/esoft-springboot:latest'
                        sh 'docker push 058264552037.dkr.ecr.ap-southeast-2.amazonaws.com/esoft-springboot:latest'
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
                git credentialsId: 'GITHUB', url: 'https://github.com/linhnm2407/esoft-test-deploy.git'
            }
        }
        stage('Update Deployment file') {
            environment {
                GIT_REPO_NAME = "esoft-test-deploy"
                GIT_USER_NAME = "linhnm2407"
            }
            steps {
                withCredentials([string(credentialsId: 'github', variable: 'GITHUB_PAN')]) {
                        sh '''
                            git config user.email "linhnm2407@gmail.com"
                            git config user.name "linhnm2407"
                            BUILD_NUMBER=${BUILD_NUMBER}
                            echo $BUILD_NUMBER
                            imageTag=$(grep -oP '(?<=esoft-springboot:)[^ ]+' values.yaml)
                            echo $imageTag
                            sed -i "s/${AWS_ECR_REPO_NAME}:${imageTag}/${AWS_ECR_REPO_NAME}:${BUILD_NUMBER}/" values.yaml
                            git add values.yaml
                            git commit -m "Update deployment Image to version \${BUILD_NUMBER}"
                            git push https://${GITHUB_TOKEN}@github.com/${GIT_USER_NAME}/${GIT_REPO_NAME} HEAD:master
                        '''
                    }
            }
        }
    }
}