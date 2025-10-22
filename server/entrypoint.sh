#!/bin/bash
set -e
exec java $JAVA_OPTS -cp .:./*:../server.jar lsfusion.server.logics.BusinessLogicsBootstrap