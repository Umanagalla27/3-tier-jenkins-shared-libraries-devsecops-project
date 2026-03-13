def call(String sonarServer) {
    withSonarQubeEnv(sonarServer) {
        sh """ $SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=${params.projectName} -Dsonar.projectKey=${params.projectKey} """
    }
}
