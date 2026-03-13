def call() {
    sh "docker rm -f ${env.dockerImageName} || true"
    sh "docker run -d --name ${env.dockerImageName} -p ${env.EXPOSE_PORT}:${env.CONTAINER_PORT} ${env.dockerHubUsername}/${env.dockerImageName}:${env.BUILD_NUMBER}"
}   