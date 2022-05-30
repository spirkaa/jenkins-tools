.POSIX:

export DOCKER_BUILDKIT=1

BASE=git.devmem.ru/projects
IMAGE_JENKINS=${BASE}/jenkins:latest
IMAGE_AGENT=${BASE}/jenkins-agent:latest

default: build

build:
	@docker build \
		--cache-from ${IMAGE_JENKINS} \
		--tag ${IMAGE_JENKINS} \
		-f .docker/Dockerfile .docker
	@docker build \
		--cache-from ${IMAGE_AGENT} \
		--tag ${IMAGE_AGENT} \
		-f .docker/agent.Dockerfile .docker

build-nocache:
	@docker build \
		--pull --no-cache \
		--tag ${IMAGE_JENKINS} \
		-f .docker/Dockerfile .docker
	@docker build \
		--pull \
		--no-cache \
		--tag ${IMAGE_AGENT} \
		-f .docker/agent.Dockerfile .docker

rmi:
	@docker rmi ${IMAGE_JENKINS} ${IMAGE_AGENT}

run:
	@docker run \
		--rm \
		--interactive \
		--tty \
		${IMAGE_AGENT}
