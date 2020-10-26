#!/bin/bash -e

if ! [ -e conf/settings.properties ]; then
  mkdir -p conf
  cat > conf/settings.properties <<EOF
db.server=localhost
db.name=lsfusion
db.user=postgres
db.password=11111
EOF
fi

command="java -cp .:./*:../server.jar"

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

command+=" lsfusion.server.logics.BusinessLogicsBootstrap"

exec $command