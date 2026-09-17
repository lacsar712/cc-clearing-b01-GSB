FROM docker.m.daocloud.io/library/alpine:latest

RUN apk add --no-cache curl

WORKDIR /seed
COPY seed.sh /seed/seed.sh
RUN chmod +x /seed/seed.sh

ENV BACKEND_URL=http://backend:8080

ENTRYPOINT ["/bin/sh", "/seed/seed.sh"]
