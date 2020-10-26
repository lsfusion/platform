#!/bin/bash -e

command="java -cp .:./*:../server.jar"

if ! [ -e conf/settings.properties ]; then
  mkdir conf
  cat > conf/settings.properties <<EOF
db.server=${DB_SERVER:-localhost}
db.name=${DB_NAME:-lsfusion}
db.user=${DB_USER:-postgres}
db.password=${DB_PASSWORD:-postgres}
EOF
  else
    if ! [ -z "$DB_USER" ]; then
    command+=" -Ddb.user=$DB_USER"
    fi

    if ! [ -z "$DB_NAME" ]; then
        command+=" -Ddb.name=$DB_NAME"
    fi

    if ! [ -z "$DB_SERVER" ]; then
        command+=" -Ddb.server=$DB_SERVER"
    fi

    if ! [ -z "$DB_PASSWORD" ]; then
        command+=" -Ddb.password=$DB_PASSWORD"
    fi
fi

if ! [ -z "$JAVA_OPTS" ]; then
    export JAVA_OPTS="$JAVA_OPTS"
fi

command+=" lsfusion.server.logics.BusinessLogicsBootstrap"

exec $command