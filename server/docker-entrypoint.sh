#!/bin/bash -e

if ! [ -e conf/settings.properties ]; then
  echo >&2 "LSFusion is not configured in $(pwd) - configuring..."

  LSFUSION_DB_HOST=${LSFUSION_DB_HOST:-postgres}
  LSFUSION_DB_USER=${LSFUSION_DB_USER:-postgres}
  LSFUSION_DB_NAME=${LSFUSION_DB_NAME:-lsfusion}

  if [ -z "$LSFUSION_DB_PASSWORD" ]; then
  	echo >&2 'error: missing required LSFUSION_DB_PASSWORD environment variable'
  	exit 1
  fi

  mkdir -p conf

  cat > conf/settings.properties <<EOF
db.server=$LSFUSION_DB_HOST
db.name=$LSFUSION_DB_NAME
db.user=$LSFUSION_DB_USER
db.password=$LSFUSION_DB_PASSWORD
EOF
fi

exec java -cp ".:/lsfusion-server.jar" lsfusion.server.logics.BusinessLogicsBootstrap
