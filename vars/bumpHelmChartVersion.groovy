def call(Map config) {
  withCredentials([
    file(credentialsId: "${GPG_KEY_CREDS_ID}", variable: 'GPG_KEY'),
    usernameColonPassword(credentialsId: "${REGISTRY_CREDS_ID}", variable: 'REGISTRY_USER'),
    ]) {
    dir('helm') {
      checkout(
        changelog: false,
        poll: false,
        scm: [$class: 'GitSCM',
          branches: [[name: '*/main']],
          extensions: [],
          userRemoteConfigs: [[
            credentialsId: "${REGISTRY_CREDS_ID}",
            url: "${HELM_CHART_GIT_URL}"
          ]]
        ]
      )

      def chart = "charts/${IMAGE_BASENAME}/Chart.yaml"
      def chartVersion =
        sh(script: "awk '/^version:/ {print \$2}' ${chart}", returnStdout: true)
          .toString()
          .trim()
      def appVersion =
        sh(script: "awk '/^appVersion:/ {print \$2}' ${chart}", returnStdout: true)
          .toString()
          .trim()
      def chartVersionBump = bumpVersion('patch', chartVersion)
      def appVersionBump = "\"${REVISION}\"".toString()
      // Checks that current app revision image present in container registry
      def registryApiUrl = "${REGISTRY_URL}/api/v1/packages/${IMAGE_OWNER}/container/${IMAGE_BASENAME}/${REVISION}"
      def (registryApiResp, registryApiRespCode) =
        sh(script: "curl -s -w '\\n%{response_code}' -u \$REGISTRY_USER ${registryApiUrl}", returnStdout: true)
          .trim()
          .tokenize("\n")

      if (!(registryApiRespCode in ['200', '404'])) {
        error "Registry response code: ${registryApiRespCode}"
      }
      else if (registryApiRespCode == '404') {
        error "Current app revision image ${REVISION} not found in registry! To trigger build use REBUILD parameter or commit in repo."
      }
      else if (appVersion == appVersionBump) {
        error "Current app revision already in chart! To trigger build commit in repo."
      }
      else if (appVersion != appVersionBump && registryApiRespCode == '200') {
        echo "Chart version: ${chartVersion} -> ${chartVersionBump}\nApp version: ${appVersion} -> ${appVersionBump}"
        sh """
          GPG_KEY_ID=\$(
            gpg --import \$GPG_KEY 2>&1 | \
            grep 'secret key imported' | \
            awk '{print \$3}' | \
            sed 's/.\$//'
          )
          git config user.signingkey \$GPG_KEY_ID
          git config commit.gpgsign true
          git config user.email jenkins@devmem.ru
          git config user.name Jenkins
          git checkout main

          sed -i 's/^version:.*/version: ${chartVersionBump}/' ${chart}
          sed -i 's/^appVersion:.*/appVersion: ${appVersionBump}/' ${chart}
        """
        withCredentials([gitUsernamePassword(credentialsId: "${REGISTRY_CREDS_ID}")]) {
          sh """
            git add ${chart}
            git commit -m "${IMAGE_BASENAME}: bump chart version"
            git push -u origin main
          """
        }
      }
    }
  }
}
