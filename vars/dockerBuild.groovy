def call() {
    // Build the Docker image
    sh "docker build -t ${env.dockerImageName} ."
     // Tag the Docker image
    sh "docker tag ${env.dockerImageName} ${env.dockerHubUsername}/${env.dockerImageName}:latest"
    sh "docker tag ${env.dockerImageName} ${env.dockerHubUsername}/${env.dockerImageName}:${env.BUILD_NUMBER}"
    // Push the Docker image 
    
    }

