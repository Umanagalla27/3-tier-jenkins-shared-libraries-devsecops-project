def call() {
    echo "🔐 Starting SBOM and Cosign Attestation..."
    
    // We attempt to use credentials if they exist, otherwise we fallback to files in the workspace
    script {
        def hasCreds = true
        try {
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
        set -e
        export COSIGN_EXPERIMENTAL=1

        if [ -z "${env.IMAGE_DIGEST}" ]; then
            echo "❌ IMAGE_DIGEST not found. Did dockerPush.groovy run?"
            exit 1
        fi

        echo "🔐 Using image digest: ${env.IMAGE_DIGEST}"

        echo "🔐 Setting up cosign..."
        # If keyPath is already 'cosign.key', we don't need to copy it
        if [ "${keyPath}" != "cosign.key" ]; then
            cp "${keyPath}" cosign.key
        fi
        chmod 600 cosign.key

        if head -1 cosign.key | grep -q "ENCRYPTED"; then
            echo "🔐 Key is encrypted"
            NEED_PASSWORD=true
        else
            echo "🔓 Key is not encrypted"
            NEED_PASSWORD=false
        fi

        echo "📦 Generating SBOM (CycloneDX)..."
        trivy image \
          --format cyclonedx \
          --output sbom.cdx.json \
          ${env.IMAGE_DIGEST}

        echo "🛡️ Running vulnerability scan..."
        trivy image \
          --ignore-unfixed \
          --format cosign-vuln \
          --output vuln.json \
          ${env.IMAGE_DIGEST}

        echo "🧾 Attesting SBOM..."
        if [ "\$NEED_PASSWORD" = "true" ]; then
            echo "${password}" | cosign attest \
                --key cosign.key \
                --type vuln \
                --predicate vuln.json \
                ${env.IMAGE_DIGEST}
        else
            cosign attest \
                --key cosign.key \
                --type vuln \
                --predicate vuln.json \
                ${env.IMAGE_DIGEST}
        fi

        echo "✍️ Signing image..."
        if [ "\$NEED_PASSWORD" = "true" ]; then
            echo "${password}" | cosign sign \
                --key cosign.key \
                --tlog=true \
                ${env.IMAGE_DIGEST}
        else
            cosign sign \
                --key cosign.key \
                --tlog=true \
                ${env.IMAGE_DIGEST}
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
