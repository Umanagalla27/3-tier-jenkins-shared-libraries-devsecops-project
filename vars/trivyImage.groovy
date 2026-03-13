def call() {
    // 1. Read the template from the Shared Library's 'resources' folder
    // Note: Do NOT use '@' or 'resources/' prefix inside libraryResource()
    def templateContent = libraryResource('html.tpl')

    // 2. Write the template to a temporary file in the current build workspace
    writeFile(file: 'trivy_tmp.tpl', text: templateContent)

    // 3. Run Trivy using the newly created local file
    sh """
      trivy image \
        --format template \
        --template @trivy_tmp.tpl \
        -o reports/trivyimage_${env.BUILD_NUMBER}.html \
        ${env.dockerHubUsername}/${env.dockerImageName}:${env.BUILD_NUMBER}
    """
}
