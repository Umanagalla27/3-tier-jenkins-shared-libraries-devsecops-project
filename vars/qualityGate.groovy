def call(credentialsId) {
    timeout(time: 3, unit: 'MINUTES') {
        def qualityGate = waitForQualityGate abortPipeline: false, credentialsId: credentialsId   
        if (qualityGate.status != 'OK') {
            error "Quality gate failed with status: ${qualityGate.status}"
        }
    }
}