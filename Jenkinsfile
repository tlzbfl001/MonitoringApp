pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo "Building on branch: ${env.GIT_BRANCH}"
                sh './gradlew build'
            }
        }

        stage('Test') {
            steps {
                echo "Running tests"
                sh './gradlew test'
            }
        }
    }
}