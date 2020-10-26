#!/bin/bash -e

if ! [ -e conf/Catalina/localhost/ROOT.xml ]; then
  mkdir -p conf/Catalina/localhost
  cat > conf/Catalina/localhost/ROOT.xml <<EOF
<?xml version='1.0' encoding='utf-8'?>
<Context>
  <Parameter name="host" value="localhost" override="false"/>
  <Parameter name="port" value="7652" override="false"/>
</Context>
EOF
fi

if ! [ -z "$SERVER_HOST" ] && ! [ -z "$SERVER_PORT" ]; then
  sed -i -e "/unpackWARs=\"true\" autoDeploy=\"true\">/a\
  <Context path=\"\"> \
    <Parameter name=\"host\" value=\"$SERVER_HOST\" override=\"false\"/> \
    <Parameter name=\"port\" value=\"$SERVER_PORT\" override=\"false\"/> \
  </Context>" conf/server.xml
fi

exec bin/catalina.sh run