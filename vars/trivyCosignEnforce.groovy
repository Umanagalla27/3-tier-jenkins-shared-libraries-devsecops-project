def call() {
    echo "🔐 Starting SBOM and Cosign Attestation..."
    
    script {
        def hasCreds = true
        try {
            // Attempt to get credentials from Jenkins
            withCredentials([
                file(credentialsId: 'COSIGN_PRIVATE_KEY', variable: 'COSIGN_KEY_FILE'),
                string(credentialsId: 'COSIGN_PASSWORD', variable: 'COSIGN_PASSWORD')
            ]) {
                runAttestation(env.COSIGN_KEY_FILE, env.COSIGN_PASSWORD)
            }
        } catch (Exception e) {
            echo "⚠️ Jenkins credentials 'COSIGN_PRIVATE_KEY' not found. Falling back to workspace files..."
            hasCreds = false
        }

        if (!hasCreds) {
            // Fallback: Check if keys exist in workspace
            if (fileExists('cosign.key')) {
                echo "✅ Found cosign.key in workspace. Proceeding with local key..."
                // Use a default password or empty if not provided in env
                def localPass = env.COSIGN_PASSWORD ?: "1234" 
                runAttestation("cosign.key", localPass)
            } else {
                error "❌ Missing Cosign keys! Please add COSIGN_PRIVATE_KEY credential or add cosign.key to the repository."
            }
        }
    }
}

def runAttestation(String keyPath, String password) {
    // Calculate qualified image name in Groovy for consistency across shell blocks
    def imageDigest = env.IMAGE_DIGEST
    def qualifiedImage = imageDigest
    
    if (imageDigest && !imageDigest.contains('/')) {
        echo "⚠️ Image digest appears unqualified. Adding repository prefix in Groovy..."
        if (env.dockerHubUsername && env.dockerImageName) {
            // Keep only the digest part
            def digestPart = imageDigest.contains('@') ? imageDigest.substring(imageDigest.indexOf('@')) : "@${imageDigest}"
            qualifiedImage = "${env.dockerHubUsername}/${env.dockerImageName}${digestPart}"
        }
    }

    echo "🔐 Using qualified image: ${qualifiedImage}"

    if (!qualifiedImage || qualifiedImage == "@") {
        error "❌ IMAGE_DIGEST not found or invalid. Did dockerPush.groovy run?"
    }

    // Step 1: Permissions and setup
    sh """
        echo "🔐 Setting up cosign keys..."
        if [ "${keyPath}" != "cosign.key" ]; then
            cp "${keyPath}" cosign.key
        fi
        chmod 600 cosign.key
    """

    // Step 2: Authentication and attestation
    withCredentials([
        usernamePassword(credentialsId: 'dockerhub-token', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')
    ]) {
        sh """
            set +e
            export COSIGN_EXPERIMENTAL=1
            export COSIGN_PASSWORD="${password}"
            
            # Login to ensure we have access for attestation
            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin

            echo "📦 Generating SBOM (CycloneDX)..."
            trivy image --format cyclonedx --output sbom.cdx.json ${qualifiedImage}

            echo "🛡️ Running vulnerability scan..."
            trivy image --ignore-unfixed --format cosign-vuln --output vuln.json ${qualifiedImage}

            echo "🧾 Attesting SBOM..."
            cosign attest --key cosign.key --type vuln --predicate vuln.json --yes ${qualifiedImage}

            if [ \$? -ne 0 ]; then
                echo "❌ Attestation failed!"
                exit 1
            fi

            echo "✍️ Signing image..."
            cosign sign --key cosign.key --tlog-upload=true --yes ${qualifiedImage}

            if [ \$? -ne 0 ]; then
                echo "❌ Signing failed!"
                exit 1
            fi
            
            echo "✅ Image signed & SBOM attested using digest"

            mkdir -p reports
            mv sbom.cdx.json reports/ || true
            mv vuln.json reports/ || true
        """
    }

    // Step 3: Cleanup
    sh """
        if [ "${keyPath}" != "cosign.key" ]; then
            rm -f cosign.key
        fi
    """
}
