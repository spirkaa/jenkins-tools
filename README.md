# Jenkins Tools

## Образы

* [Jenkins](.docker/Dockerfile) с плагинами ([JCasC](https://www.jenkins.io/projects/jcasc/))
* [Jenkins Agent](.docker/agent.Dockerfile) с утилитами

## Библиотека инструментов ([Shared Library](https://www.jenkins.io/doc/book/pipeline/shared-libraries/))

* [buildDockerImage](vars/buildDockerImage.groovy) - сборка образа и пуш в закрытый реестр (1 или 2 метки)
* [bumpVersion](vars/bumpVersion.groovy) - увеличение значения версии [semver](https://semver.org/)
* [bumpHelmChartVersion](vars/bumpHelmChartVersion.groovy) - изменение версий Helm чарта и приложения в отдельном репозитории с подписью коммита
