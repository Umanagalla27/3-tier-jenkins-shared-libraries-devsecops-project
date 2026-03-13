def call(String buildStatus, String emailAddress) {

    def buildUser = 'Unknown'
    def cause = currentBuild.getBuildCauses()[0]
    if (cause) {
        buildUser = cause.userId ?: cause.userName ?: cause.shortDescription
    }

    emailext(
        subject: "Pipeline ${buildStatus}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """
            <p>Project: ${env.JOB_NAME}</p>
            <p>Build Number: ${env.BUILD_NUMBER}</p>
            <p>Status: ${buildStatus}</p>
            <p>Started by: ${buildUser}</p>

            <p><a href="${env.BUILD_URL}">View Build</a></p>
        """,
        to: "${emailAddress}",
        from: "${emailAddress}",
        mimeType: 'text/html',
        attachmentsPattern: 'reports/*.json, reports/*.html'
    )

}