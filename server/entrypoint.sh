#!/bin/bash -e

if ! [ -e conf/settings.properties ]; then
  echo >&2 "lsFusion server is not configured in $(pwd) - configuring..."

  LSFUSION_DB_USER=${LSFUSION_DB_USER:-postgres}
  LSFUSION_DB_NAME=${LSFUSION_DB_NAME:-lsfusion}

  if [ -z "$LSFUSION_DB_PASSWORD" ]; then
  	echo >&2 'error: missing required LSFUSION_DB_PASSWORD environment variable'
  	exit 1
  fi

  if [ -z "$LSFUSION_DB_HOST" ]; then
  	echo >&2 'error: missing required LSFUSION_DB_HOST environment variable'
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

exec java -cp ".:server.jar" lsfusion.server.logics.BusinessLogicsBootstrap