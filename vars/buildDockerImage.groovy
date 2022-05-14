def call(Map config) {
  String dockerFile = config.dockerFile
  String tag = config.tag
  String altTag = null
  if(config.altTag) {
    altTag = config.altTag
  }
  String registryUrl = env.REGISTRY_URL
  if(config.registryUrl) {
    registryUrl = config.registryUrl
  }
  String registryCredsId = env.REGISTRY_CREDS_ID
  if(config.registryCredsId) {
    registryCredsId = config.registryCredsId
  }
  String imageFullname = env.IMAGE_FULLNAME
  if(config.imageFullname) {
    imageFullname = config.imageFullname
  }
  String labelCreated = env.LABEL_CREATED
  if(config.labelCreated) {
    labelCreated = config.labelCreated
  }
  String labelTitle = env.LABEL_TITLE
  if(config.labelTitle) {
    labelTitle = config.labelTitle
  }
  String labelDescription = env.LABEL_DESCRIPTION
  if(config.labelDescription) {
    labelDescription = config.labelDescription
  }
  String labelAuthors = env.LABEL_AUTHORS
  if(config.labelAuthors) {
    labelAuthors = config.labelAuthors
  }
  String labelUrl = env.LABEL_URL
  if(config.labelUrl) {
    labelUrl = config.labelUrl
  }
  String labelSource = env.GIT_URL
  if(config.labelSource) {
    labelSource = config.labelSource
  }
  String labelRevision = env.REVISION
  if(config.labelRevision) {
    labelRevision = config.labelRevision
  }
  String context = '.'
  if(config.context) {
    context = config.context
  }
  boolean useCache = config.useCache
  String cache = "--pull --no-cache"
  String cacheFrom = "${imageFullname}:${tag}"
  if(useCache) {
    if(config.cacheFrom) {
      cacheFrom = config.cacheFrom
    }
    cache = "--cache-from ${cacheFrom}"
  }

  docker.withRegistry("${registryUrl}", "${registryCredsId}") {
    env.DOCKER_BUILDKIT = 1
    def myImage = docker.build(
      "${imageFullname}:${tag}",
      "--label \"org.opencontainers.image.created=${labelCreated}\" \
      --label \"org.opencontainers.image.title=${labelTitle}\" \
      --label \"org.opencontainers.image.description=${labelDescription}\" \
      --label \"org.opencontainers.image.authors=${labelAuthors}\" \
      --label \"org.opencontainers.image.url=${labelUrl}\" \
      --label \"org.opencontainers.image.source=${labelSource}\" \
      --label \"org.opencontainers.image.version=${tag}\" \
      --label \"org.opencontainers.image.revision=${labelRevision}\" \
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
