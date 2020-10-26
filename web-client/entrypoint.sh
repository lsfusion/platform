#!/bin/bash -e

if ! [ -e conf/Catalina/localhost/ROOT.xml ]; then

  SERVER_HOST=${SERVER_HOST:-localhost}
  SERVER_PORT=${SERVER_PORT:-7652}

  mkdir -p conf/Catalina/localhost
  cat > conf/Catalina/localhost/ROOT.xml <<EOF
<?xml version='1.0' encoding='utf-8'?>
<Context>
  <Parameter name="host" value="$SERVER_HOST" override="false"/>
  <Parameter name="port" value="$SERVER_PORT" override="false"/>
</Context>
EOF
  elif ! [ -z "$SERVER_HOST" ] && ! [ -z "$SERVER_PORT" ]; then
    sed -i -e "/unpackWARs=\"true\" autoDeploy=\"true\">/a\
    <Context path=\"\"> \
      <Parameter name=\"host\" value=\"$SERVER_HOST\" override=\"false\"/> \
      <Parameter name=\"port\" value=\"$SERVER_PORT\" override=\"false\"/> \
    </Context>" conf/server.xml
fi

if ! [ -z "$JAVA_OPTIONS" ]; then
    export JAVA_OPTS="$JAVA_OPTIONS"
fi

exec bin/catalina.sh run