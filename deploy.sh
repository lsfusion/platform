export MAVEN_OPTS=-Xms512m -Xmx4024m -XX:MaxPermSize=512m

mvn deploy

cd desktop-client
mvn deploy -P assemble,sign,pack

cd ../server
mvn deploy -P assemble,pack

cd ../web-client
mvn deploy -P gwt,desktop,war
