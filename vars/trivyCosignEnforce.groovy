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
    sh """
        set +e
        export COSIGN_EXPERIMENTAL=1
        export COSIGN_PASSWORD="${password}"

        # Ensure IMAGE_DIGEST is qualified with the repository name if it's not already
        FINAL_IMAGE="${env.IMAGE_DIGEST}"
        if [[ ! "\$FINAL_IMAGE" =~ "/" ]]; then
            echo "⚠️ Image digest appears unqualified. Adding repository prefix..."
            # Try to construct complete image name from environment variables
            if [ ! -z "${env.dockerHubUsername}" ] && [ ! -z "${env.dockerImageName}" ]; then
                FINAL_IMAGE="${env.dockerHubUsername}/${env.dockerImageName}@\${FINAL_IMAGE#*@}"
            fi
        fi

        echo "🔐 Using image digest: \${FINAL_IMAGE}"

        if [ -z "\${FINAL_IMAGE}" ]; then
            echo "❌ IMAGE_DIGEST not found. Did dockerPush.groovy run?"
            exit 1
        fi

        echo "🔐 Setting up cosign..."
        if [ "${keyPath}" != "cosign.key" ]; then
            cp "${keyPath}" cosign.key
        fi
        chmod 600 cosign.key

        echo "📦 Generating SBOM (CycloneDX)..."
        trivy image \
          --format cyclonedx \
          --output sbom.cdx.json \
          \${FINAL_IMAGE}

        echo "🛡️ Running vulnerability scan..."
        trivy image \
          --ignore-unfixed \
          --format cosign-vuln \
          --output vuln.json \
          \${FINAL_IMAGE}

        echo "🧾 Attesting SBOM..."
        # Use --yes to skip confirmation for private repos
        cosign attest \
            --key cosign.key \
            --type vuln \
            --predicate vuln.json \
            --yes \
            \${FINAL_IMAGE}

        if [ \$? -ne 0 ]; then
            echo "❌ Attestation failed!"
            exit 1
        fi

        echo "✍️ Signing image..."
        cosign sign \
            --key cosign.key \
            --tlog-upload=true \
            --yes \
            \${FINAL_IMAGE}

        if [ \$? -ne 0 ]; then
            echo "❌ Signing failed!"
            exit 1
        fi

        # Cleanup only if we copied it
        if [ "${keyPath}" != "cosign.key" ]; then
            rm -f cosign.key
        fi
        echo "✅ Image signed & SBOM attested using digest"

        mkdir -p reports
        mv sbom.cdx.json reports/ || true
        mv vuln.json reports/ || true
    """
}
