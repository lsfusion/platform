export MAVEN_OPTS="-Xms512m -Xmx4096m -XX:MaxPermSize=512m"

svn update

mvn deploy

cd desktop-client
mvn deploy -P assemble,sign,pack
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases -DgroupId=lsfusion.platform -Dversion=1.0.1 -Dfile=lsfusion-client-1.0.1.jar -DartifactId=desktop-client -Dpackaging=jar -Dclassifier=assembly
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases -DgroupId=lsfusion.platform -Dversion=1.0.1 -Dfile=lsfusion-client-1.0.1.jar.pack.gz -DartifactId=desktop-client -Dpackaging=pack.gz -Dclassifier=assembly

cd ../server
mvn deploy -P assemble,pack
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases -DgroupId=lsfusion.platform -Dversion=1.0.1 -Dfile=lsfusion-server-1.0.1.jar -DartifactId=server -Dpackaging=jar -Dclassifier=assembly
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases -DgroupId=lsfusion.platform -Dversion=1.0.1 -Dfile=lsfusion-server-1.0.1.jar.pack.gz -DartifactId=server -Dpackaging=pack.gz -Dclassifier=assembly

cd ../web-client
mvn deploy -P gwt,desktop,war
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases -DgroupId=lsfusion.platform -Dversion=1.0.1 -Dfile=lsfusion-client-1.0.1.war -DartifactId=web-client -Dpackaging=war
