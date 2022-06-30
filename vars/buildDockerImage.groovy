def call(Map config) {
  String dockerFile = config?.dockerFile ?: 'Dockerfile'
  String context = config?.context ?: '.'
  String tag = config?.tag ?: 'latest'
  String altTag = config?.altTag ?: null
  String registryUrl = config?.registryUrl ?: env.REGISTRY_URL
  String registryCredsId = config?.registryCredsId ?: env.REGISTRY_CREDS_ID
  String imageFullname = config?.imageFullname ?: env.IMAGE_FULLNAME
  String labelCreated = config?.labelCreated ?: env.LABEL_CREATED
  String labelTitle = config?.labelTitle ?: env.LABEL_TITLE
  String labelDescription = config?.labelDescription ?: env.LABEL_DESCRIPTION
  String labelAuthors = config?.labelAuthors ?: env.LABEL_AUTHORS
  String labelUrl = config?.labelUrl ?: env.LABEL_URL
  String labelSource = config?.labelSource ?: env.GIT_URL
  String labelRevision = config?.labelRevision ?: env.REVISION
  boolean useCache = config.useCache
  String cache = "--pull --no-cache"
  String cacheFrom = "${imageFullname}:${tag}"
  if(useCache) {
    if(config.cacheFrom) {
      cacheFrom = config.cacheFrom
    }
    cache = "--cache-from ${cacheFrom}"
  }
  boolean pushToRegistry = true
  if(config.pushToRegistry == 'no') {
    pushToRegistry = false
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
    if(pushToRegistry) {
      myImage.push()
      if(altTag) {
        myImage.push(altTag)
      }
      sh "docker rmi -f \$(docker inspect -f '{{ .Id }}' ${myImage.id})"
    }
  }
}
