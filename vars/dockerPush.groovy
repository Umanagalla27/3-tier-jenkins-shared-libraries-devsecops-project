def call() {
    withCredentials([
        usernamePassword(
            credentialsId: 'dockerhub-token',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
        )
    ]) {
        sh """
            set -e
            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin

            docker push ${env.dockerHubUsername}/${env.dockerImageName}:latest
            docker push ${env.dockerHubUsername}/${env.dockerImageName}:${env.BUILD_NUMBER}
        """

        script {
            env.IMAGE_DIGEST = sh(
                script: "docker inspect --format='{{index .RepoDigests 0}}' ${env.dockerHubUsername}/${env.dockerImageName}:${env.BUILD_NUMBER}",
                returnStdout: true
            ).trim()

            echo "🔐 Resolved Image Digest: ${env.IMAGE_DIGEST}"
        }
    }
}
