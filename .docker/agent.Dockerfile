# hadolint global ignore=DL3007,DL3008

FROM jenkins/agent:latest

SHELL [ "/bin/bash", "-euxo", "pipefail", "-c" ]

USER root

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends \
        gnupg \
        ca-certificates \
        curl \
        jq \
        make \
    && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/bin

RUN VERSION="$(curl -fsSL -o /dev/null -w %\{url_effective\} https://github.com/mikefarah/yq/releases/latest | sed 's/^.*\///g' )" \
    && curl -fsSL -o yq "https://github.com/mikefarah/yq/releases/download/${VERSION}/yq_linux_amd64" \
    && chmod +x /usr/bin/yq \
    && yq --version | grep -E "${VERSION}"

USER jenkins
WORKDIR /home/jenkins
