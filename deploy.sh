export MAVEN_OPTS="-Xms512m -Xmx4096m -XX:MaxPermSize=512m"

svn update

mvn deploy

cd desktop-client
mvn deploy -P assemble,sign,pack

cd ../server
mvn deploy -P assemble,pack

cd ../web-client
mvn deploy -P gwt,desktop,war

