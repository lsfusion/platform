set BIN_JAR=shef-0.5.jar
set SRC_JAR=shef-0.5-sources.jar
set POM=shef-0.5.pom

set GROUP=net.atlanticbb
set ARTIFACT=shef
set VERSION=0.5

if "%SRC_JAR%" == "" (
    @echo "no sources"
) else (
    call mvn deploy:deploy-file -Dfile=%SRC_JAR% -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases/ -DgroupId=%GROUP% -DartifactId=%ARTIFACT% -Dversion=%VERSION% -Dpackaging=jar -Dclassifier=sources
)

if "%POM%" == "" (
    call mvn deploy:deploy-file -Dfile=%BIN_JAR% -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases/ -DgroupId=%GROUP% -DartifactId=%ARTIFACT% -Dversion=%VERSION% -Dpackaging=jar -DgeneratePom=true
) else (
    call mvn deploy:deploy-file -Dfile=%BIN_JAR%  -DpomFile=%POM% -DrepositoryId=lsfusion -Durl=http://lsfusion.ru/nexus/content/repositories/releases/ -DgroupId=%GROUP% -DartifactId=%ARTIFACT% -Dversion=%VERSION% -Dpackaging=jar
)

