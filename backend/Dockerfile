# backend/Dockerfile

FROM ocaml/opam:alpine-3.22-ocaml-5.3 AS ocaml-builder
USER root
RUN apk add --no-cache postgresql-dev
USER opam
WORKDIR /home/opam/ocaml-engine

COPY --chown=opam:opam ocaml-engine/ ./

RUN rm -rf _build \
    && opam install -y dune yojson postgresql \
    && opam exec -- dune build \
    && install -D _build/default/bin/worker.exe /tmp/worker

FROM gradle:8.7-jdk21-alpine AS java-builder
WORKDIR /src/backend

COPY backend/ ./
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache libpq \
    && addgroup -S send \
    && adduser -S -G send send
WORKDIR /app

COPY --from=java-builder /src/backend/build/libs/*.jar app.jar
COPY --from=ocaml-builder /tmp/worker /app/worker
RUN chown send:send /app/app.jar /app/worker \
    && chmod 0555 /app/worker

USER send
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
