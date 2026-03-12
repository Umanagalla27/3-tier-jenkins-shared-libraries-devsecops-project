def call(buildStatus) {

    // def buildStatus = currentBuild.currentResult ?: 'SUCCESS'

    slackSend(
        channel: params.slackChannel,
        color: buildStatus == 'SUCCESS' ? 'good' : 'danger',
        message: """
                    Build status: ${buildStatus}
                Job: ${env.JOB_NAME}
                Build: #${env.BUILD_NUMBER}
                ${env.BUILD_URL}
                """
    )
}
