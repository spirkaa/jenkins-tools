def call(Map config) {
  def gpgKeyCredsId = config?.gpgKeyCredsId ?: "${GPG_KEY_CREDS_ID}"
  def chartGitCredsId = config?.chartGitCredsId ?: "${REGISTRY_CREDS_ID}"
  def imgRegistryCredsId = config?.imgRegistryCredsId ?: "${chartGitCredsId}"

  def chartGitUrl = config?.chartGitUrl ?: "${HELM_CHART_GIT_URL}"
  def chartGitBranch = config?.chartGitBranch ?: 'main'

  def chartName = config?.chartName ?: "${IMAGE_BASENAME}"
  def chartYaml = config?.chartYaml ?: "charts/${chartName}/Chart.yaml"
  def chartVersionBumpStep = config?.chartVersionBumpStep ?: 'patch'

  def imgOwner = config?.imgOwner ?: "${IMAGE_OWNER}"
  def imgName = config?.imgName ?: "${chartName}"
  def imgRevision = config?.imgRevision ?: "${REVISION}"
  def imgRegistryBase = config?.imgRegistryBase ?: "${REGISTRY_URL}"
  def imgRegistryApiUrl =
    config?.imgRegistryApiUrl
    ?: "${imgRegistryBase}/api/v1/packages/${imgOwner}/container/${imgName}/${imgRevision}"

  def gitConfigUserEmail = config?.gitConfigUserEmail ?: 'jenkins@devmem.ru'
  def gitConfigUserName = config?.gitConfigUserName ?: 'Jenkins'
  def gitCommitMsg = config?.gitCommitMsg ?: "${chartName}: bump chart version"

  withCredentials([
    file(credentialsId: "${gpgKeyCredsId}", variable: 'gpgKey'),
    usernameColonPassword(credentialsId: "${imgRegistryCredsId}", variable: 'imgRegistryCreds'),
    ]) {
    dir('helm') {
      checkout(
        changelog: false,
        poll: false,
        scm: [$class: 'GitSCM',
          branches: [[name: "*/${chartGitBranch}"]],
          extensions: [],
          userRemoteConfigs: [[
            credentialsId: "${chartGitCredsId}",
            url: "${chartGitUrl}"
          ]]
        ]
      )

      def chartVersion =
        sh(script: "awk '/^version:/ {print \$2}' ${chartYaml}",
          returnStdout: true).toString().trim()
      def appVersion =
        sh(script: "awk '/^appVersion:/ {print \$2}' ${chartYaml}",
          returnStdout: true).toString().trim()
      def chartVersionBump = bumpVersion(chartVersionBumpStep, chartVersion)
      def appVersionBump = "\"${imgRevision}\""
      if (appVersion == appVersionBump) {
        unstable 'Current app revision already in chart! ' \
          + 'To trigger new image build commit in app repo.'
        return
      }

      // Checks that current app revision image present in container registry
      def (resp, respCode) =
        sh(script: "curl -s -w '\\n%{response_code}' -u \$imgRegistryCreds ${imgRegistryApiUrl}",
          returnStdout: true).trim().tokenize("\n")
      if (!(respCode in ['200', '404'])) {
        error "Registry response code: ${respCode}"
      }
      else if (respCode == '404') {
        error "Current app revision image ${imgRevision} not found in registry! " \
          + 'To trigger image build use REBUILD parameter or commit in app repo.'
      }
      else if (appVersion != appVersionBump && respCode == '200') {
        echo "Chart version: ${chartVersion} -> ${chartVersionBump}\n" \
          + "App version: ${appVersion} -> ${appVersionBump}"
        sh """
          GPG_KEY_ID=\$(
            gpg --import \$gpgKey 2>&1 | \
            grep 'secret key imported' | \
            awk '{print \$3}' | \
            sed 's/.\$//'
          )
          git config user.signingkey \$GPG_KEY_ID
          git config user.email ${gitConfigUserEmail}
          git config user.name ${gitConfigUserName}
          git checkout ${chartGitBranch}

          sed -i 's/^version:.*/version: ${chartVersionBump}/' ${chartYaml}
          sed -i 's/^appVersion:.*/appVersion: ${appVersionBump}/' ${chartYaml}
        """
        withCredentials([gitUsernamePassword(credentialsId: "${chartGitCredsId}")]) {
          sh """
            git add ${chartYaml}
            git commit -S -m "${gitCommitMsg}"
            git push -u origin ${chartGitBranch}
          """
        }
      }
    }
  }
}
