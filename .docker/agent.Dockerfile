FROM jenkins/agent:latest

USER root

RUN set -eux \
    && apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends \
        gnupg \
        ca-certificates \
        curl \
        jq \
        make \
    && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux \
    && cd /tmp \
    && BINARY=yq_linux_amd64 \
    && VERSION="$(curl -fsS -L -o /dev/null -w %{url_effective} https://github.com/mikefarah/yq/releases/latest | sed 's/^.*\///g' )" \
    && curl -fsS -L -O https://github.com/mikefarah/yq/releases/download/${VERSION}/${BINARY}.tar.gz \
    && tar xvfz ${BINARY}.tar.gz \
    && mv ${BINARY} /usr/bin/yq \
    && chmod +x /usr/bin/yq \
    && rm -rf /tmp/* \
    && yq --version | grep -E "$(echo ${VERSION} | cut -c 2-)"

USER jenkins
