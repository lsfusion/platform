#!/bin/bash -e

if ! [ -e /usr/share/lsfusion4-client/conf/Catalina/localhost/ROOT.xml ]; then
  echo >&2 "lsFusion client is not configured - configuring..."

    if [ -z "$LSFUSION_SERVER_HOST" ]; then
  	echo >&2 'error: missing required LSFUSION_SERVER_HOST environment variable'
  	exit 1
    fi

    if [ -z "$LSFUSION_SERVER_PORT" ]; then
  	echo >&2 'error: missing required LSFUSION_SERVER_PORT environment variable'
  	exit 1
    fi

  cat > /usr/share/lsfusion4-client/conf/Catalina/localhost/ROOT.xml <<EOF
<?xml version='1.0' encoding='utf-8'?>
<Context path="" docBase="../lsfusion">
  <Parameter name="host" value="$LSFUSION_SERVER_HOST" override="false"/>
  <Parameter name="port" value="$LSFUSION_SERVER_PORT" override="false"/>
</Context>
EOF
fi

sh /usr/share/lsfusion4-client/bin/catalina.sh run