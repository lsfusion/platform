FROM alpine:latest

ARG FILENAME

WORKDIR /logics

COPY ${FILENAME} ./lsfusion.jar

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
