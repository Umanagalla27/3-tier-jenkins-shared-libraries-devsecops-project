def call() {
    withCredentials([usernamePassword(
        credentialsId: 'github-token',
        usernameVariable: 'GIT_USER',
        passwordVariable: 'GIT_TOKEN'
                                    )]) {
        sh '''
            git config --local user.name "$gitUserConfigName"
            git config --local user.email "$gitUserConfigEmail"
            git config --local credential.helper "!f() { echo username=$GIT_USER; echo password=$GIT_TOKEN; }; f"
            
            git checkout "$BRANCH"
            sed -i "s|image: .*|image: $dockerHubUsername/$dockerImageName:$BUILD_NUMBER|" "$MANIFESTFILENAME"
            git add "$MANIFESTFILENAME"
            git commit -m "$BUILD_NUMBER"
            git push origin "$BRANCH"
            git checkout "$gitBranch"
        '''
    }
}