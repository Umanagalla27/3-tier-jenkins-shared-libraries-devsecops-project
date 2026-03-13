def call() {
    sh "gitleaks detect --source . -r reports/gitleaks-report.json -f json || true"
}
