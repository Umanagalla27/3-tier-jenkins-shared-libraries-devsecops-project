def call() {
    withCredentials([
        file(credentialsId: 'COSIGN_PRIVATE_KEY', variable: 'COSIGN_KEY_FILE'),
        string(credentialsId: 'COSIGN_PASSWORD', variable: 'COSIGN_PASSWORD')
    ]) {
        sh """
            set -e
            export COSIGN_EXPERIMENTAL=1

            if [ -z "${env.IMAGE_DIGEST}" ]; then
                echo "❌ IMAGE_DIGEST not found. Did dockerPush.groovy run?"
                exit 1
            fi

            echo "🔐 Using image digest: ${env.IMAGE_DIGEST}"

            echo "🔐 Setting up cosign..."
            cp "\$COSIGN_KEY_FILE" cosign.key
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
                echo "\$COSIGN_PASSWORD" | cosign attest \
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
                echo "\$COSIGN_PASSWORD" | cosign sign \
                    --key cosign.key \
                    ${env.IMAGE_DIGEST}
            else
                cosign sign \
                    --key cosign.key \
                    ${env.IMAGE_DIGEST}
            fi

            rm -f cosign.key cosign.pub
            echo "✅ Image signed & SBOM attested using digest"

            mv sbom.cdx.json reports/
            mv vuln.json reports/
        """
    }
}
