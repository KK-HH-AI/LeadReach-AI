pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Setup Registry') {
            steps {
                sh 'pnpm config set registry https://registry.npmmirror.com'
            }
        }
        stage('Install') {
            steps {
                dir('frontend') {
                    sh 'pnpm install --frozen-lockfile'
                }
            }
        }
        stage('Build') {
            steps {
                dir('frontend') { sh 'pnpm build' }
            }
        }
        stage('Approval') {
            steps {
                input message: 'Build done. Deploy?', ok: 'Deploy'
            }
        }
        stage('Deploy') {
            steps {
                sh '''
                    docker cp frontend/dist/. nginx:/usr/share/nginx/html/
                    docker exec -u root nginx chmod -R 755 /usr/share/nginx/html
                    docker exec -u root nginx find /usr/share/nginx/html -type f -exec chmod 644 {} \\;
                '''
            }
        }
    }
}