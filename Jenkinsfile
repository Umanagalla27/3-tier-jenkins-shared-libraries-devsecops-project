@Library('Jenkins_shared_library') _
def COLOR_MAP = [
    'FAILURE' : 'danger',
    'SUCCESS' : 'good'
]

pipeline{
    agent any
    parameters {
        choice(name: 'action', choices: 'create\ndelete', description: 'Select action to perform (create/delete).')
        
        string(name: 'gitUrl', defaultValue: 'https://github.com/Umanagalla27/3-tier-jenkins-shared-libraries-devsecops-project.git', description: 'Git URL')
        string(name: 'gitBranch', defaultValue: 'database', description: 'Git Branch')

        string(name: 'projectName', defaultValue: 'database', description: 'Project Name')
        string(name: 'projectKey', defaultValue: 'database', description: 'Project Key')

        string(name: 'dockerHubUsername', defaultValue: 'uma1827', description: 'Docker Hub Username')
        string(name: 'dockerImageName', defaultValue: 'mysql-signed', description: 'Docker Image Name')

        string(name: 'gitUserConfigName', defaultValue: 'umanagalla27', description: 'Git User Name')
        string(name: 'gitUserConfigEmail', defaultValue: 'nagallauma88@gmail.com', description: 'Git User Email')
        string(name: 'gitUserName', defaultValue: 'umanagalla27', description: 'Git User Name')
        string(name: 'gitPassword', defaultValue: 'github-token', description: 'Git Password')

        string(name: 'slackChannel', defaultValue: '#jenkins-shared-devsecops', description: 'Slack Channel')
        string(name: 'emailAddress', defaultValue: 'uman32247@gmail.com', description: 'Email Address')
    }

    tools{
        jdk 'jdk17'
    }
    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        BRANCH = 'deployment'
        MANIFESTFILENAME = '05-three-tier-app/04-statefulset.yaml'
        sonarServer = 'sonar-server'
        sonarqubeCredentialsId = 'sonar-token'
        dockerHubUsername = "${params.dockerHubUsername}"
        dockerImageName = "${params.dockerImageName}"
    }
    stages{

        stage('Clean Workspace'){
            steps{
                cleanWorkspace()
            }
        }
        stage('checkout from Git'){
            when { expression { params.action == 'create'}}    

            steps{
                checkoutGit(params.gitUrl, params.gitBranch)
            }
        }
        stage('gitleak'){
            when { expression { params.action == 'create'}}    
            steps{
                gitleak()
            }
        }
        stage('sonarqube Analysis'){
        when { expression { params.action == 'create'}}    
            steps{
                sonarqubeAnalysis(sonarServer)
            }
        }

        stage('sonarqube QualitGate'){
        when { expression { params.action == 'create'}}    
            steps{
                sonarqubequalitygate(sonarqubeCredentialsId)
                }
            }
        
        stage('Trivy file scan'){
        when { expression { params.action == 'create'}}    
            steps{
                trivyFs()
            }
        }


        stage('Docker Build'){
        when { expression { params.action == 'create'}}    
            steps{
                dockerBuild()
            }
        }


        stage('Trivy Image Scan'){
        when { expression { params.action == 'create'}}    
            steps{
                trivyImage()
            }
        }

        stage('Docker Push To DockerHub'){
        when { expression { params.action == 'create'}}    
            steps{
                dockerPush()
            }
        }


        stage('SBOM and Cosign Attestation'){
            when { expression { params.action == 'create'}}    
            steps{
                trivyCosignEnforce()
            }
        }


        stage('Manual Approval') {
            steps {
                manualwithslack()
            }
        }



        stage('update k8s deployment frontend file') {
            when {
                allOf {
                    expression { env.APPROVED == "true" }
                    expression { params.action == 'create' }
                }
            }
            steps {
                updateK8sDeploymentFile()
            }
        }
    }

    post {
        always {
            script {

                def buildStatus = currentBuild.currentResult

                zpostslack(buildStatus)
                zpostemail(buildStatus, params.emailAddress)

            }
        }
    }
}
