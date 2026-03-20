Deploy 3-Tier DevSecOps Pipeline with Jenkins Shared Libraries

## Summary of changes
- The project is a complete React-based frontend application with a production-grade DevSecOps CI/CD pipeline
- Infrastructure setup includes AWS EC2 with Jenkins, Docker, SonarQube, and security tools (Trivy, Cosign, Gitleaks)
- The pipeline implements security at every stage: secret scanning, SAST, SCA, container scanning, SBOM generation, and image signing
- Deployment uses Kubernetes (EKS) with GitOps (ArgoCD) and monitoring (Prometheus/Grafana)
- Jenkins Shared Libraries provide reusable pipeline functions for clean, maintainable automation

## Execution Steps

### Step 1: AWS Infrastructure Setup
Provision EC2 instance and configure security groups for all required services.
#### 1.1: Launch and configure EC2 instance
Create an AWS EC2 instance with appropriate specifications and configure security groups to allow access to Jenkins, SonarQube, and the application.
- Launch EC2 instance (c5a.large or c5a.xlarge) with Ubuntu 22.04 LTS
- Configure 30 GB EBS storage
- Create/update security group to allow ports: 22 (SSH), 80 (HTTP), 443 (HTTPS), 8080 (Jenkins), 9000 (SonarQube)
- Connect to instance via SSH and verify connectivity

#### 1.2: Install system dependencies and common tools
Update the system and install essential packages required for the DevSecOps toolchain.
- Run system update: `sudo apt update && sudo apt upgrade -y`
- Install common tools: bash-completion, wget, git, zip, unzip, curl, jq, net-tools, build-essential, ca-certificates, apt-transport-https, gnupg
- Install latest Git from PPA: `sudo add-apt-repository ppa:git-core/ppa`
- Verify Git installation and reload bash completion

### Step 2: Core Software Installation
Install Java, Jenkins, Docker, and Node.js as the foundation for the CI/CD pipeline.
#### 2.1: Install Java Development Kit
Install OpenJDK 17 which is required by Jenkins and other tools in the pipeline.
- Install OpenJDK 17: `sudo apt install -y openjdk-17-jdk`
- Verify Java installation: `java --version`
- Set JAVA_HOME environment variable if needed

#### 2.2: Install and configure Jenkins
Set up Jenkins CI/CD server with proper permissions and initial configuration.
- Add Jenkins repository GPG key and apt source
- Install Jenkins: `sudo apt install -y jenkins`
- Start and enable Jenkins service: `sudo systemctl enable --now jenkins`
- Retrieve initial admin password: `sudo cat /var/lib/jenkins/secrets/initialAdminPassword`
- Access Jenkins UI at `http://<instance-ip>:8080` and complete setup wizard

#### 2.3: Install Docker and configure permissions
Install Docker for building and running containerized applications.
- Add Docker's official GPG key and repository
- Install Docker CE, CLI, containerd, buildx, and compose plugins
- Add ubuntu, current user, and jenkins user to docker group
- Restart Jenkins to apply docker group membership: `sudo systemctl restart jenkins`
- Verify Docker installation: `docker ps`

#### 2.4: Install Node.js for frontend builds
Install Node.js 20 required for building the React application.
- Add NodeSource repository for Node.js 20
- Install Node.js and npm: `sudo apt install -y nodejs`
- Verify installation: `node --version` and `npm --version`

### Step 3: Security Tools Installation
Install security scanning and signing tools required for DevSecOps pipeline.
#### 3.1: Install Trivy vulnerability scanner
Set up Trivy for scanning filesystems and container images for vulnerabilities.
- Add Trivy repository GPG key
- Add Trivy apt source to sources.list
- Install Trivy: `sudo apt-get install trivy`
- Verify Trivy installation: `trivy --version`

#### 3.2: Install Cosign for image signing
Install Cosign to implement software supply chain security with image signatures and attestations.
- Download latest Cosign binary for Linux
- Move to /usr/local/bin and make executable
- Generate Cosign key pair: `cosign generate-key-pair`
- Set password for private key (e.g., 1234) and save both cosign.key and cosign.pub files

#### 3.3: Install Gitleaks for secret scanning
Install Gitleaks to detect hardcoded secrets and credentials in source code.
- Install Gitleaks: `sudo apt install gitleaks -y`
- Verify installation: `gitleaks version`

### Step 4: SonarQube Setup
Deploy SonarQube for continuous code quality and security analysis.
#### 4.1: Run SonarQube container
Launch SonarQube as a Docker container with persistent volumes for data, logs, and extensions.
- Create SonarQube container with ports mapped: `docker run -d --name sonarqube -p 9000:9000 -v sonarqube_data:/opt/sonarqube/data -v sonarqube_logs:/opt/sonarqube/logs -v sonarqube_extensions:/opt/sonarqube/extensions sonarqube:26.2.0.119303-community`
- Wait for SonarQube to start (check logs: `docker logs -f sonarqube`)
- Access SonarQube at `http://<instance-ip>:9000`
- Login with default credentials (admin/admin) and change password on first login

#### 4.2: Configure SonarQube for Jenkins integration
Create authentication token and webhook for Jenkins integration.
- In SonarQube UI, navigate to Administration → Security → Users
- Generate token for admin user and save it securely
- Navigate to Administration → Configuration → Webhooks
- Create webhook pointing to Jenkins: `http://<jenkins-ip>:8080/sonarqube-webhook/`

### Step 5: AWS and Kubernetes Tools
Install AWS CLI, kubectl, eksctl, and Helm for Kubernetes cluster management.
#### 5.1: Install AWS CLI
Install AWS Command Line Interface for managing AWS resources.
- Download AWS CLI v2: `curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"`
- Unzip and install: `unzip awscliv2.zip && sudo ./aws/install`
- Verify installation: `aws --version`

#### 5.2: Install kubectl
Install Kubernetes command-line tool for cluster operations.
- Add Kubernetes apt repository GPG key
- Add Kubernetes apt source for v1.35
- Install kubectl and bash-completion: `sudo apt-get install -y kubectl bash-completion`
- Enable kubectl auto-completion and alias in .bashrc
- Verify installation: `kubectl version --client`

#### 5.3: Install eksctl
Install eksctl for creating and managing EKS clusters.
- Download latest eksctl binary for Linux AMD64
- Extract and install to /usr/local/bin
- Enable eksctl auto-completion in .bashrc
- Verify installation: `eksctl version`

#### 5.4: Install Helm package manager
Install Helm for deploying Kubernetes applications via charts.
- Add Helm repository GPG key
- Add Helm apt source
- Install Helm: `sudo apt-get install helm bash-completion`
- Enable Helm auto-completion in .bashrc
- Verify installation: `helm version`

### Step 6: External Service Accounts
Create and configure external service accounts for AWS, Docker Hub, GitHub, and Slack.
#### 6.1: Configure AWS IAM and credentials
Create AWS IAM user for Jenkins with appropriate permissions.
- Login to AWS Console and navigate to IAM
- Create IAM user named "jenkins"
- Attach policies: AmazonEKSClusterPolicy, AmazonEC2ContainerRegistryFullAccess (or custom policy)
- Create access key under Security credentials and save Access Key ID and Secret Access Key
- Run `aws configure` and enter credentials, default region (ap-south-1), and output format (json)
- Verify configuration: `aws configure list`

#### 6.2: Setup Docker Hub access token
Create Docker Hub personal access token for pushing images.
- Login to Docker Hub
- Navigate to Account Settings → Security
- Create New Access Token with appropriate permissions
- Save token securely for Jenkins credentials

#### 6.3: Create GitHub personal access token
Generate GitHub token for repository access and webhook management.
- Login to GitHub and navigate to Settings → Developer settings → Personal access tokens → Tokens (classic)
- Generate new token with scopes: repo, admin:repo_hook, notifications
- Save token securely for Jenkins credentials

#### 6.4: [NO-CODE-CHANGE] Setup Slack workspace and webhook (optional)
Configure Slack for pipeline notifications if desired.
- Create or use existing Slack workspace
- Create channel: #jenkins-shared-devsecops
- Add Jenkins CI app from Slack marketplace
- Configure app for the channel and copy webhook token
- Save token for Jenkins credentials

#### 6.5: [NO-CODE-CHANGE] Setup Gmail app password (optional)
Create Gmail app password for email notifications if desired.
- Login to Gmail account
- Navigate to Account → Security → 2-Step Verification → App passwords
- Generate app password for "Mail" application
- Save password for Jenkins credentials

### Step 7: Jenkins Plugin Installation
Install all required Jenkins plugins for the DevSecOps pipeline.
#### 7.1: Install Jenkins plugins via UI
Access Jenkins and install necessary plugins for pipeline functionality.
- Navigate to Manage Jenkins → Plugins → Available
- Install Eclipse Temurin installer Plugin
- Install Email Extension Template Plugin
- Install OWASP Dependency-Check Plugin
- Install Pipeline: Stage View Plugin
- Install SonarQube Scanner for Jenkins
- Install NodeJS plugin
- Install Pipeline Utility Steps
- Install Slack Notification plugin
- Restart Jenkins if required

### Step 8: Jenkins Tools Configuration
Configure Java, Node.js, SonarQube Scanner, and Dependency-Check tools in Jenkins.
#### 8.1: Configure JDK installations
Set up Java Development Kit for pipeline jobs.
- Navigate to Manage Jenkins → Tools → JDK installations
- Add JDK with name "jdk17"
- Select Install automatically from Eclipse Temurin
- Choose version 17 (latest)

#### 8.2: Configure Node.js installations
Set up Node.js for frontend build tasks.
- Navigate to Manage Jenkins → Tools → NodeJS installations
- Add NodeJS with name "node20"
- Select Install automatically
- Choose version 20.x (latest)

#### 8.3: Configure SonarQube Scanner
Set up SonarQube Scanner tool for code analysis.
- Navigate to Manage Jenkins → Tools → SonarQube Scanner installations
- Add SonarQube Scanner with name "sonar-scanner"
- Select Install automatically from Maven Central
- Choose latest version

#### 8.4: Configure Dependency-Check
Set up OWASP Dependency-Check tool for vulnerability scanning.
- Navigate to Manage Jenkins → Tools → Dependency-Check installations
- Add Dependency-Check with name "dp-check"
- Select Install automatically
- Choose latest version

### Step 9: Jenkins Credentials Management
Store all sensitive credentials securely in Jenkins credential store.
#### 9.1: Add SonarQube authentication token
Store SonarQube token for authentication.
- Navigate to Manage Jenkins → Credentials → System → Global credentials
- Click Add Credentials
- Kind: Secret text
- Secret: paste SonarQube token from Step 4.2
- ID: sonar-token
- Description: SonarQube authentication token

#### 9.2: Add Docker Hub credentials
Store Docker Hub username and access token for image pushing.
- Add Credentials
- Kind: Username with password
- Username: Docker Hub username
- Password: Docker Hub access token from Step 6.2
- ID: dockerhub-token
- Description: Docker Hub credentials

#### 9.3: Add GitHub credentials
Store GitHub username and personal access token for repository operations.
- Add Credentials
- Kind: Username with password
- Username: GitHub username
- Password: GitHub personal access token from Step 6.3
- ID: github-token
- Description: GitHub credentials

#### 9.4: Add Cosign private key and password
Store Cosign signing key and password for image attestation.
- Add Credentials for private key
- Kind: Secret file
- File: Upload cosign.key file from Step 3.2
- ID: COSIGN_PRIVATE_KEY
- Description: Cosign private key for image signing
- Add Credentials for password
- Kind: Secret text
- Secret: Cosign key password (e.g., 1234)
- ID: COSIGN_PASSWORD
- Description: Cosign private key password

#### 9.5: [NO-CODE-CHANGE] Add Slack credentials (optional)
Store Slack webhook token for notifications if configured in Step 6.4.
- Add Credentials
- Kind: Secret text
- Secret: Slack webhook token
- ID: slackcred
- Description: Slack webhook for notifications

#### 9.6: [NO-CODE-CHANGE] Add email credentials (optional)
Store Gmail credentials for email notifications if configured in Step 6.5.
- Add Credentials
- Kind: Username with password
- Username: Gmail email address
- Password: Gmail app password
- ID: mail-cred
- Description: Email notification credentials

### Step 10: Jenkins System Configuration
Configure SonarQube server connection and notification settings.
#### 10.1: Configure SonarQube server connection
Connect Jenkins to SonarQube instance for code analysis integration.
- Navigate to Manage Jenkins → System → SonarQube servers
- Click Add SonarQube
- Name: sonar-server
- Server URL: `http://<instance-ip>:9000` or `http://localhost:9000`
- Server authentication token: select sonar-token credential
- Save configuration

#### 10.2: [NO-CODE-CHANGE] Configure email notifications (optional)
Set up SMTP configuration for email notifications if credentials added in Step 9.6.
- Navigate to Manage Jenkins → System → Extended E-mail Notification
- SMTP server: smtp.gmail.com
- SMTP Port: 465
- Use SSL: checked
- Credentials: select mail-cred
- Default user e-mail suffix: @gmail.com
- Configure E-mail Notification section
- SMTP server: smtp.gmail.com
- Use SMTP Authentication: checked
- User Name: Gmail email address
- Password: select mail-cred credential
- Use TLS: checked
- SMTP Port: 587

#### 10.3: [NO-CODE-CHANGE] Configure Slack notifications (optional)
Set up Slack workspace and channel for pipeline notifications if configured in Step 6.4.
- Navigate to Manage Jenkins → System → Slack
- Workspace: enter workspace name
- Credential: select slackcred
- Default channel: #jenkins-shared-devsecops
- Test connection and save

### Step 11: Jenkins Shared Library Configuration
Configure Jenkins to load the shared library containing reusable pipeline functions.
#### 11.1: Add Global Pipeline Library
Configure Jenkins to load shared library from the repository.
- Navigate to Manage Jenkins → System → Global Pipeline Libraries
- Click Add
- Name: Jenkins_shared_library (must match @Library directive in Jenkinsfile)
- Default version: main (or @main based on repository branch)
- Retrieval method: Modern SCM
- Source Code Management: Git
- Project Repository: enter your forked/cloned repository URL
- Credentials: select github-token
- Save configuration

### Step 12: Repository Setup
Fork or clone the project repository and prepare it for pipeline execution.
#### 12.1: [NO-CODE-CHANGE] Fork/clone repository
Obtain the project repository for pipeline execution.
- Fork the repository: https://github.com/Umanagalla27/3-tier-jenkins-shared-libraries-devsecops-project.git
- Or clone to local machine and push to your own repository
- Ensure all files are present: Jenkinsfile, Dockerfile, package.json, vars/ directory, src/ directory
- Verify cosign.key and cosign.pub exist in repository root (or will use Jenkins credentials)
- Note the repository URL and main branch name for pipeline configuration

### Step 13: Jenkins Pipeline Job Creation
Create the Jenkins pipeline job that will execute the DevSecOps workflow.
#### 13.1: Create pipeline job with parameters
Set up Jenkins pipeline job with all required parameters for the build.
- Click New Item in Jenkins
- Enter name: Frontend-DevSecOps-Pipeline
- Select Pipeline and click OK
- Check "This project is parameterized"
- Add Choice Parameter: name=action, choices=create\ndelete, description="Select action to perform"
- Add String Parameters with defaults from Jenkinsfile (gitUrl, gitBranch, projectName, projectKey, dockerHubUsername, dockerImageName, gitUserConfigName, gitUserConfigEmail, slackChannel, emailAddress)
- In Pipeline section, select "Pipeline script from SCM"
- SCM: Git
- Repository URL: your repository URL
- Credentials: github-token
- Branch Specifier: */frontend (or */main)
- Script Path: Jenkinsfile
- Save configuration

### Step 14: EKS Cluster Provisioning
Create Amazon EKS cluster for deploying the containerized application.
#### 14.1: Create EKS cluster without node group
Provision the EKS control plane in AWS.
- Run: `eksctl create cluster --name my-cluster --region ap-south-1 --version 1.34 --without-nodegroup`
- Wait for cluster creation to complete (approximately 15-20 minutes)
- Verify cluster status in AWS Console or via `eksctl get cluster`

#### 14.2: Create managed node group
Add worker nodes to the EKS cluster for running workloads.
- Run: `eksctl create nodegroup --cluster my-cluster --name my-nodes-ng --nodes 3 --nodes-min 3 --nodes-max 6 --node-type t3.medium --region ap-south-1`
- Wait for node group creation to complete
- Update kubeconfig: `aws eks update-kubeconfig --name my-cluster --region ap-south-1`
- Verify nodes: `kubectl get nodes`

#### 14.3: Associate IAM OIDC provider
Enable IAM roles for service accounts (IRSA) for the cluster.
- Run: `eksctl utils associate-iam-oidc-provider --cluster my-cluster --approve`
- Verify OIDC provider is created in AWS IAM

### Step 15: AWS Load Balancer Controller
Install AWS Load Balancer Controller for managing ALB/NLB ingress resources.
#### 15.1: Create IAM policy for Load Balancer Controller
Create IAM policy with required permissions for ALB controller.
- Download IAM policy JSON: `curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.14.0/docs/install/iam_policy.json`
- Create IAM policy: `aws iam create-policy --policy-name AWSLoadBalancerControllerIAMPolicy --policy-document file://iam_policy.json`
- Note the policy ARN from output

#### 15.2: Create IAM service account for controller
Create Kubernetes service account with IAM role for the controller.
- Replace <ACCOUNT_ID> with your AWS account ID in the command
- Run: `eksctl create iamserviceaccount --cluster=my-cluster --namespace=kube-system --name=aws-load-balancer-controller --attach-policy-arn=arn:aws:iam::<ACCOUNT_ID>:policy/AWSLoadBalancerControllerIAMPolicy --override-existing-serviceaccounts --region ap-south-1 --approve`
- Verify service account: `kubectl get sa aws-load-balancer-controller -n kube-system`

#### 15.3: Install Load Balancer Controller via Helm
Deploy the AWS Load Balancer Controller using Helm chart.
- Add Helm repository: `helm repo add eks https://aws.github.io/eks-charts`
- Update repositories: `helm repo update`
- Install controller: `helm install aws-load-balancer-controller eks/aws-load-balancer-controller -n kube-system --set clusterName=my-cluster --set serviceAccount.create=false --set serviceAccount.name=aws-load-balancer-controller --set region=ap-south-1`
- Verify deployment: `kubectl get deployment -n kube-system aws-load-balancer-controller`

### Step 16: Prometheus and Grafana Installation
Install monitoring stack for observing cluster and application metrics.
#### 16.1: Install Prometheus stack via Helm
Deploy kube-prometheus-stack for comprehensive monitoring.
- Add Helm repository: `helm repo add prometheus-community https://prometheus-community.github.io/helm-charts`
- Update repositories: `helm repo update`
- Create namespace: `kubectl create namespace prometheus`
- Install stack: `helm install stable prometheus-community/kube-prometheus-stack -n prometheus`
- Wait for all pods to be ready: `kubectl get pods -n prometheus -w`

#### 16.2: Expose Prometheus and Grafana services
Make Prometheus and Grafana accessible via LoadBalancer.
- Patch Prometheus service: `kubectl patch svc stable-kube-prometheus-sta-prometheus -n prometheus -p '{"spec": {"type": "LoadBalancer"}}'`
- Patch Grafana service: `kubectl patch svc stable-grafana -n prometheus -p '{"spec": {"type": "LoadBalancer"}}'`
- Get LoadBalancer URLs: `kubectl get svc -n prometheus`
- Access Grafana with credentials: username=admin, password=prom-operator
- Import dashboards: Kubernetes Monitoring (12740), Node Exporter (1860), Namespace Views (15758)

### Step 17: ArgoCD Installation
Install ArgoCD for GitOps-based continuous deployment.
#### 17.1: Deploy ArgoCD via Helm
Install ArgoCD in the cluster for managing deployments.
- Add Helm repository: `helm repo add argo https://argoproj.github.io/argo-helm`
- Update repositories: `helm repo update`
- Create namespace: `kubectl create namespace argocd`
- Install ArgoCD: `helm install argocd argo/argo-cd --namespace argocd`
- Wait for pods: `kubectl get pods -n argocd -w`

#### 17.2: Expose ArgoCD server and retrieve credentials
Make ArgoCD UI accessible and get login credentials.
- Patch service to LoadBalancer: `kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'`
- Get ArgoCD URL: `kubectl get svc argocd-server -n argocd -o json | jq --raw-output '.status.loadBalancer.ingress[0].hostname'`
- Get admin password: `kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d`
- Login to ArgoCD UI with username=admin and the retrieved password

### Step 18: Pipeline Execution
Execute the DevSecOps pipeline to build, scan, sign, and prepare the application for deployment.
#### 18.1: Configure and run Jenkins pipeline
Trigger the pipeline with appropriate parameters.
- Navigate to Frontend-DevSecOps-Pipeline job in Jenkins
- Click "Build with Parameters"
- Set action: create
- Set gitUrl to your repository URL
- Set gitBranch: frontend (or main)
- Set dockerHubUsername to your Docker Hub username
- Set dockerImageName: frontend-signed
- Configure other parameters (email, Slack, Git user info) as needed
- Click Build
- Monitor pipeline execution through Jenkins Blue Ocean or classic view

#### 18.2: Monitor pipeline stages and approve deployment
Track pipeline progress through all DevSecOps stages and approve for deployment.
- Monitor stages: Clean Workspace, Git Checkout, Gitleaks, SonarQube Analysis, Quality Gate, npm install, Trivy FS Scan, OWASP Dependency Check, Docker Build, Trivy Image Scan, Docker Push, Docker Run, SBOM & Cosign Attestation
- When pipeline reaches Manual Approval stage, review Slack notification (if configured) or Jenkins UI
- Click Approve to proceed with K8s manifest update
- Verify pipeline completes successfully
- Check Docker Hub for signed image with tag matching BUILD_NUMBER

#### 18.3: [NO-CODE-CHANGE] Verify image signature and attestation
Confirm the Docker image was properly signed with Cosign.
- Get image digest from pipeline logs or Docker Hub
- Run: `cosign verify --key cosign.pub <dockerhub-username>/frontend-signed@sha256:<digest>`
- Verify attestation: `cosign verify-attestation --key cosign.pub <dockerhub-username>/frontend-signed@sha256:<digest>`
- Confirm SBOM was generated and attached to image

### Step 19: ArgoCD Application Deployment
Configure ArgoCD to deploy the application to Kubernetes cluster.
#### 19.1: Create ArgoCD application for frontend
Set up GitOps application in ArgoCD pointing to the deployment manifests.
- Login to ArgoCD UI
- Click New App
- Application Name: frontend-app
- Project: default
- Sync Policy: Automatic (enable auto-sync and self-heal)
- Repository URL: your repository URL
- Revision: deployment (branch updated by pipeline)
- Path: 05-three-tier-app (or path containing K8s manifests)
- Cluster URL: https://kubernetes.default.svc
- Namespace: default (or create three-tier namespace)
- Click Create
- ArgoCD will sync and deploy the application automatically

#### 19.2: [NO-CODE-CHANGE] Verify application deployment
Confirm the application is running successfully in the cluster.
- Check ArgoCD UI for sync status (should show Healthy and Synced)
- Verify pods: `kubectl get pods`
- Verify services: `kubectl get svc`
- Get application LoadBalancer URL: `kubectl get svc frontend-service -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'` (adjust service name as needed)
- Access application in browser using LoadBalancer URL
- Verify application functionality

### Step 20: Monitoring and Validation
Confirm monitoring is operational and review application metrics.
#### 20.1: [NO-CODE-CHANGE] Access Grafana dashboards
View cluster and application metrics in Grafana.
- Access Grafana LoadBalancer URL from Step 16.2
- Login with admin/prom-operator
- Navigate to imported dashboards
- Review Kubernetes Monitoring dashboard (12740) for cluster health
- Review Node Exporter dashboard (1860) for node metrics
- Review Namespace dashboard (15758) for application metrics
- Confirm metrics are being collected and displayed correctly

#### 20.2: [NO-CODE-CHANGE] Validate security scan results
Review security scan reports from the pipeline execution.
- Login to SonarQube and review project analysis results
- Check for code quality issues, vulnerabilities, and code smells
- Review Trivy scan reports in Jenkins job artifacts or console output
- Review OWASP Dependency-Check report for vulnerable dependencies
- Confirm all security gates passed or review failures for remediation
