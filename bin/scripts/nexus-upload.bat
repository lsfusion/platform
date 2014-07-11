set GROUP=bibliothek.gui
set ARTIFACT=dockingFramesCore
set VERSION=1.1.2p10f

set BIN_JAR=dockingFramesCore-1.1.2p10f.jar
set POM=dockingFramesCore-1.1.2p10f.pom
set SRC_JAR=dockingFramesCore-1.1.2p10f-sources.jar

set REPO_ID=lsfusion.thirdparty
set REPO_URL=http://lsfusion.ru/nexus/content/repositories/thirdparty/

if "%SRC_JAR%" == "" (
    @echo "no sources"
) else (
    call mvn deploy:deploy-file -Dfile=%SRC_JAR% -DrepositoryId=%REPO_ID% -Durl=%REPO_URL% -DgroupId=%GROUP% -DartifactId=%ARTIFACT% -Dversion=%VERSION% -Dpackaging=jar -Dclassifier=sources
)

if "%POM%" == "" (
    call mvn deploy:deploy-file -Dfile=%BIN_JAR% -DrepositoryId=%REPO_ID% -Durl=%REPO_URL% -DgroupId=%GROUP% -DartifactId=%ARTIFACT% -Dversion=%VERSION% -Dpackaging=jar -DgeneratePom=true
) else (
    call mvn deploy:deploy-file -Dfile=%BIN_JAR%  -DpomFile=%POM% -DrepositoryId=%REPO_ID% -Durl=%REPO_URL% -DgroupId=%GROUP% -DartifactId=%ARTIFACT% -Dversion=%VERSION% -Dpackaging=jar
)
