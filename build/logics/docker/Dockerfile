FROM alpine:latest

ARG FILENAME

WORKDIR /logics

COPY ${FILENAME} ./lsfusion.jar

ENTRYPOINT ["sh", "-c", "cp -r /logics/* /lsfusion/ && tail -f /dev/null"]
