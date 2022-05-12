def call(Map config) {
  String dockerFile = config.dockerFile
  String tag = config.tag
  String altTag = null
  if(config.altTag) {
    altTag = config.altTag
  }
  String context = '.'
  if(config.context) {
    context = config.context
  }
  boolean useCache = config.useCache
  String cache = "--pull --no-cache"
  if(useCache) {
    cache = "--cache-from ${env.IMAGE_FULLNAME}:${tag}"
  }

  docker.withRegistry("${env.REGISTRY_URL}", "${env.REGISTRY_CREDS_ID}") {
    env.DOCKER_BUILDKIT = 1
    def myImage = docker.build(
      "${env.IMAGE_FULLNAME}:${tag}",
      "--label \"org.opencontainers.image.created=${env.LABEL_CREATED}\" \
      --label \"org.opencontainers.image.title=${env.LABEL_TITLE}\" \
      --label \"org.opencontainers.image.description=${env.LABEL_DESCRIPTION}\" \
      --label \"org.opencontainers.image.authors=${env.LABEL_AUTHORS}\" \
      --label \"org.opencontainers.image.url=${env.LABEL_URL}\" \
      --label \"org.opencontainers.image.source=${env.GIT_URL}\" \
      --label \"org.opencontainers.image.version=${tag}\" \
      --label \"org.opencontainers.image.revision=${env.REVISION}\" \
      --progress=plain \
      ${cache} \
      -f ${dockerFile} ${context}"
    )
    myImage.push()
    if(altTag) {
      myImage.push(altTag)
    }
    sh "docker rmi -f \$(docker inspect -f '{{ .Id }}' ${myImage.id})"
  }
}
