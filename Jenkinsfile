pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '10', daysToKeepStr: '60'))
    parallelsAlwaysFailFast()
    disableConcurrentBuilds()
  }

    triggers {
      cron(BRANCH_NAME == 'main' ? 'H 8 * * 6' : '')
    }

  environment {
    REGISTRY = 'git.devmem.ru'
    REGISTRY_URL = "https://${REGISTRY}"
    REGISTRY_CREDS_ID = 'gitea-user'
    IMAGE_OWNER = 'projects'
    IMAGE_BASENAME = 'jenkins'
    IMAGE_FULLNAME = "${REGISTRY}/${IMAGE_OWNER}/${IMAGE_BASENAME}"
    IMAGE_TAG = 'latest'
    DOCKERFILE = '.docker/Dockerfile'
    LABEL_AUTHORS = 'Ilya Pavlov <piv@devmem.ru>'
    LABEL_TITLE = 'Jenkins'
    LABEL_DESCRIPTION = 'Jenkins'
    LABEL_URL = 'https://www.jenkins.io'
    LABEL_CREATED = sh(script: "date '+%Y-%m-%dT%H:%M:%S%:z'", returnStdout: true).toString().trim()
    REVISION = GIT_COMMIT.take(7)
  }

  stages {
    stage('Build') {
      parallel {
        stage('Build jenkins image (cache)') {
          when {
            allOf {
              branch 'main'
              anyOf {
                changeset ".docker/**"
                changeset "Jenkinsfile"
              }
              not {
                anyOf {
                  triggeredBy 'TimerTrigger'
                  triggeredBy cause: 'UserIdCause'
                  changeRequest()
                }
              }
            }
          }
          steps {
            script {
              buildDockerImage(
                dockerFile: "${DOCKERFILE}",
                tag: "${IMAGE_TAG}",
                context: ".docker",
                useCache: true
              )
            }
          }
        }

        stage('Build jenkins image (no cache)') {
          when {
            branch 'main'
            anyOf {
              triggeredBy 'TimerTrigger'
              triggeredBy cause: 'UserIdCause'
            }
          }
          steps {
            script {
              buildDockerImage(
                dockerFile: "${DOCKERFILE}",
                tag: "${IMAGE_TAG}",
                context: ".docker"
              )
            }
          }
        }

        stage('Build jenkins-agent image (cache)') {
          when {
            allOf {
              branch 'main'
              anyOf {
                changeset ".docker/**"
                changeset "Jenkinsfile"
              }
              not {
                anyOf {
                  triggeredBy 'TimerTrigger'
                  triggeredBy cause: 'UserIdCause'
                  changeRequest()
                }
              }
            }
          }
          steps {
            script {
              def IMAGE_BASENAME = 'jenkins-agent'
              def IMAGE_FULLNAME = "${REGISTRY}/${IMAGE_OWNER}/${IMAGE_BASENAME}"
              def DOCKERFILE = ".docker/agent.Dockerfile"
              buildDockerImage(
                dockerFile: "${DOCKERFILE}",
                tag: 'latest',
                context: ".docker",
                useCache: true,
                imageFullname: "${IMAGE_FULLNAME}",
                labelTitle: "${IMAGE_BASENAME}",
                labelDescription: "${IMAGE_BASENAME}",
                labelUrl: 'https://github.com/jenkinsci/docker-agent'
              )
            }
          }
        }

        stage('Build jenkins-agent image (no cache)') {
          when {
            branch 'main'
            anyOf {
              triggeredBy 'TimerTrigger'
              triggeredBy cause: 'UserIdCause'
            }
          }
          steps {
            script {
              def IMAGE_BASENAME = 'jenkins-agent'
              def IMAGE_FULLNAME = "${REGISTRY}/${IMAGE_OWNER}/${IMAGE_BASENAME}"
              def DOCKERFILE = ".docker/agent.Dockerfile"
              buildDockerImage(
                dockerFile: "${DOCKERFILE}",
                tag: 'latest',
                context: ".docker",
                imageFullname: "${IMAGE_FULLNAME}",
                labelTitle: "${IMAGE_BASENAME}",
                labelDescription: "${IMAGE_BASENAME}",
                labelUrl: 'https://github.com/jenkinsci/docker-agent'
              )
            }
          }
        }
      }
    }
  }

  post {
    always {
      emailext(
        to: '$DEFAULT_RECIPIENTS',
        subject: '$DEFAULT_SUBJECT',
        body: '$DEFAULT_CONTENT'
      )
    }
  }
}
