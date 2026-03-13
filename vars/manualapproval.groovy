def call() {
    try {
        timeout(time: 10, unit: 'MINUTES') {
            input message: 'Approve to update the k8s deployment frontend file'
        }
        env.APPROVED = "true"
    } catch (err) {
        echo "⏭️ Approval not granted (aborted or timeout). Skipping deployment."
        env.APPROVED = "false"
    }
}
