def call(String versionType, String currentVersion) {
  def split = currentVersion.split('\\.')
  switch(versionType) {
      case 'patch':
      split[2]=1+Integer.parseInt(split[2])
      break
      case 'minor':
      split[1]=1+Integer.parseInt(split[1])
      split[2]=0
      break
      case 'major':
      split[0]=1+Integer.parseInt(split[0])
      split[1]=0
      split[2]=0
      break
  }
  return split.join('.')
}
