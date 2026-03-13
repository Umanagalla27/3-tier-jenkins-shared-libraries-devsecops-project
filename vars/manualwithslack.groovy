def call() {
    // Send Slack notification when approval required
    slackSend(
        channel: params.slackChannel,
        color: 'warning',
        message: """
        🚨 *Manual Approval Required*
        Job: ${env.JOB_NAME}
        Build: #${env.BUILD_NUMBER}

        Approve deployment for frontend K8s update.

        👉 Open Jenkins:
        ${env.BUILD_URL}input
        """
    )

    try {
        timeout(time: 10, unit: 'MINUTES') {
            input message: "Approve to update the k8s deployment ${env.MANIFESTFILENAME} file"
        }

        env.APPROVED = "true"

        slackSend(
            channel: params.slackChannel,
            color: 'good',
            message: "✅ Deployment Approved - ${env.MANIFESTFILENAME} - Build #${env.BUILD_NUMBER}"
        )

    } catch (err) {

        echo "⏭️ Approval not granted (aborted or timeout). Skipping deployment."
        env.APPROVED = "false"

        slackSend(
            channel: params.slackChannel,
            color: 'danger',
            message: "❌ Deployment NOT approved (timeout/abort) - ${env.MANIFESTFILENAME} - Build #${env.BUILD_NUMBER}"
        )
    }
}