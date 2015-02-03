Name "lsFusion Platform"

SetCompressor lzma

# General Symbol Definitions
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION 1.2.0
!define COMPANY luxsoft
!define URL lsfusion.ru

!define PLATFORM_SECTION_NAME "lsFusion Platform"
!define SERVER_SECTION_NAME "lsFusion Server"
!define CLIENT_SECTION_NAME "lsFusion Client"
!define WEBCLIENT_SECTION_NAME "lsFusion Web Client"
!define MENU_SECTION_NAME "Start Menu Items"
!define SERVICES_SECTION_NAME "Create services"
!define PG_SECTION_NAME "PostgreSQL ${PG_VERSION}"
!define JAVA_SECTION_NAME "JDK ${JDK_VERSION}"
!define TOMCAT_SECTION_NAME "Apache Tomcat 7.0.47"
!define IDEA_SECTION_NAME "IntelliJ IDEA Community Edition ${IDEA_VERSION} with lsFusion plugin"

!define CLIENT_JAR "lsfusion-client-${VERSION}.jar"
!define SERVER_JAR "lsfusion-server-${VERSION}.jar"
!define SERVER_LIBRARY_NAME "lsfusion-server-${VERSION}"
!define WEBCLIENT_WAR "lsfusion-client-${VERSION}.war"

!define INSTBINDIR "$INSTDIR\install-bin"
!define INSTCONFDIR "$INSTDIR\install-config"

# MUI Symbol Definitions
!define MUI_ICON resources\lsfusion.ico
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_UNICON resources\lsfusion.ico
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_LANGDLL_REGISTRY_ROOT HKLM
!define MUI_LANGDLL_REGISTRY_KEY ${REGKEY}
!define MUI_LANGDLL_REGISTRY_VALUENAME InstallerLanguage
; !define MUI_LANGDLL_ALWAYSSHOW 1

# Needed Variables
Var javaVersion
Var javaDir
Var javaHome
Var javaExe
Var jvmDll

Var pgHost
Var pgPort
Var pgDbName
Var pgUser
Var pgPassword
Var pgVersion
Var pgDir
Var pgServiceName

Var ideaDir

Var createShortcuts
Var createServices

Var tomcatVersion
Var tomcatDir
Var tomcatShutdownPort
Var tomcatHttpPort
Var tomcatAjpPort
Var tomcatServiceName 

Var platformServerHost
Var platformServerPort
Var platformServerPassword
Var platformServiceName
Var webClientContext
Var webClientDirectory

# Included files
!include Sections.nsh
!include MUI2.nsh
!include LogicLib.nsh
!include TextFunc.nsh

!include StrFunc.nsh
${StrRep}

!include WordFunc.nsh
!include Utils.nsh

# Installer pages
!insertmacro MUI_PAGE_WELCOME

# TODO License page
;!insertmacro MUI_PAGE_LICENSE $(lsLicense)

!define MUI_PAGE_CUSTOMFUNCTION_LEAVE pageComponentsLeave
!insertmacro MUI_PAGE_COMPONENTS

# PG pages
Page custom pgConfigPagePre pgConfigPageLeave
!insertmacro CustomDirectoryPage $(strPostgreDirHeader) $(strPostgreDirTextTop) $(strDestinationFolder) $pgDir pgPagePre

# Tomcat pages
Page custom tomcatConfigPagePre tomcatConfigPageLeave
!insertmacro CustomDirectoryPage $(strTomcatDirHeader) $(strTomcatDirTextTop) $(strDestinationFolder) $tomcatDir tomcatPagePre

# Java pages
; !insertmacro CustomDirectoryPage $(strJavaDirHeader) $(strJavaDirTextTop) $(strDestinationFolder) $javaDir javaPagePre
Page custom javaExistingDirPagePre javaExistingDirPageLeave

# IntelliJ Idea pages
!insertmacro CustomDirectoryPage $(strIdeaDirHeader) $(strIdeaDirTextTop) $(strDestinationFolder) $ideaDir ideaPagePre

# Platform pages
Page custom platformConfigPagePre platformConfigPageLeave

!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

# Uninstaller pages
!insertmacro MUI_UNPAGE_COMPONENTS
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Reserved Files
!insertmacro MUI_RESERVEFILE_LANGDLL
ReserveFile "${NSISDIR}\Plugins\AdvSplash.dll"

# Installer languages
!insertmacro MUI_LANGUAGE English
!insertmacro MUI_LANGUAGE Russian
!include I18nEn.nsh
!include I18nRu.nsh

LicenseLangString lsLicense ${LANG_ENGLISH} "resources\license-english.txt"
LicenseLangString lsLicense ${LANG_RUSSIAN} "resources\license-russian.txt"

# Installer attributes
OutFile ${OUT_FILE}
InstallDir "$ProgramFiles${ARCH}\lsFusion Platform"
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion 1.2.0.0
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName "lsFusion Platform"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription ""
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright ""
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

!include FusionSections.nsh
!include PgFunctions.nsh
!include TomcatFunctions.nsh
!include JavaFunctions.nsh
!include PlatformFunctions.nsh

!include Uninstaller.nsh

Function pageComponentsLeave
    ${ifNot} ${SectionIsSelected} ${SecServer}
    ${andIfNot} ${SectionIsSelected} ${SecClient}
    ${andIfNot} ${SectionIsSelected} ${SecWebClient}
        MessageBox MB_ICONEXCLAMATION|MB_OK $(strPlatformIsNotSelected)
        Abort
    ${endIf}
    
    ${ifNot} ${SectionIsSelected} ${SecTomcat}
        StrCpy $tomcatDir ""
    ${endIf}

    ${if} ${SectionIsSelected} ${SecShortcuts}
        StrCpy $createShortcuts "1"
    ${else}
        StrCpy $createShortcuts "0"
    ${endIf}
    
    ${if} ${SectionIsSelected} ${SecServices}
        StrCpy $createServices "1"
    ${else}
        StrCpy $createServices "0"
    ${endIf}
FunctionEnd

# Installer functions
Function .onInit
    SetRegView ${ARCH}
    
    InitPluginsDir
    
    Call checkUserAdmin

    Push $R1
    File /oname=$PLUGINSDIR\spltmp.bmp resources\lsfusion.bmp
    advsplash::show 1000 600 400 -1 $PLUGINSDIR\spltmp
    Pop $R1
    Pop $R1
    
    StrCpy $javaDir "$ProgramFiles${ARCH}\Java\jdk${JDK_VERSION}"

    StrCpy $ideaDir "$ProgramFiles32\JetBrains\IDEA Community Edition ${IDEA_VERSION}"

    StrCpy $pgDir "$ProgramFiles${ARCH}\PostgreSQL\${PG_VERSION}"
    StrCpy $pgHost "localhost"
    StrCpy $pgPort "5432"
    StrCpy $pgUser "postgres"
    StrCpy $pgDbName "lsfusion"
    StrCpy $pgServiceName "postgresql-${PG_VERSION}"

    StrCpy $tomcatDir "$ProgramFiles${ARCH}\apache-tomcat-7.0.47"
    StrCpy $tomcatShutdownPort "8005"
    StrCpy $tomcatHttpPort "8080"
    StrCpy $tomcatAjpPort "8009"
    StrCpy $tomcatServiceName "Tomcat7"

    StrCpy $platformServerPort "7652"
    StrCpy $platformServiceName "lsfusion-server"
    StrCpy $webClientContext "lsfusion"
    
    !insertmacro MUI_LANGDLL_DISPLAY

    Call initJavaFromRegistry

    ; if installed Java is outdated then install the new one.
    ${ifNot} $javaVersion == ""
        !insertmacro DisableSection ${SecJava}
    ${endIf}

    ; Check if Tomcat is installed
    EnumRegKey $1 HKLM "SOFTWARE\Apache Software Foundation\Tomcat\" "0"
    ${if} $1 == "7.0"
        !insertmacro DisableSection ${SecTomcat}
    ${else}
        StrCpy $tomcatVersion ""
    ${endIf}
    
    ; Check if PostgreSQL is installed
    EnumRegKey $1 HKLM "SOFTWARE\PostgreSQL\Installations\" "0"
    ${if} $1 != ""
        ReadRegStr $pgVersion HKLM "SOFTWARE\PostgreSQL\Installations\$1" "Version"
        ReadRegStr $pgDir HKLM "SOFTWARE\PostgreSQL\Installations\$1" "Base Directory"
        ; if installed version is < PG_VERSION then abort
        ${VersionCompare} $pgVersion "${PG_VERSION}" $0
        ${if} $0 == "2"
            MessageBox MB_ICONEXCLAMATION|MB_OK $(strOldPostgreMessage)
            Abort
        ${else}
            !insertmacro DisableSection ${SecPG}
        ${endIf}
    ${endIf}
    
    !insertmacro ExpandSection ${SecPlatform}

    !ifndef DEV
        !insertmacro HideSection ${SecIdea}
    !else
        !insertmacro UnselectSection ${SecServices}
    !endif
FunctionEnd

Section -post SecPost
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    
    SetOutPath $INSTDIR

    WriteUninstaller $INSTDIR\uninstall.exe
    
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1

    Call execAntConfiguration
    
    CALL createServices

    ${if} $createShortcuts == "1"
        CALL createShortcuts
    ${endIf}
SectionEnd

Function CheckUserAdmin
    ClearErrors
    UserInfo::GetName
    ${if} ${Errors}
        MessageBox MB_OK "Error! This DLL can't run under Windows 9x!"
        Quit
    ${endIf}
    
    Pop $0
    UserInfo::GetAccountType
    Pop $1
  
    ${ifNot} $1 == "Admin"
        MessageBox MB_OK|MB_ICONSTOP "$(strUserShouldBeAdmin)"
        Quit
    ${endIf}
FunctionEnd

Function initJavaFromRegistry
    ClearErrors

    ReadRegStr $javaVersion HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
    ReadRegStr $javaHome HKLM "SOFTWARE\JavaSoft\Java Development Kit\$javaVersion" "JavaHome"
    ReadRegStr $jvmDll HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$javaVersion" "RuntimeLib"
    ${VersionCompare} $javaVersion "${JDK_MAJORVERSION}" $0
    
    ${if} $0 == "2"
    ${orIf} ${Errors}
       StrCpy $javaVersion ""
       StrCpy $javaHome ""
       StrCpy $jvmDll ""
   ${endIf}
FunctionEnd


Function execAntConfiguration
    SetOutPath ${INSTCONFDIR}

    DetailPrint "Configuring lsFusion"
    
    ${ConfigWriteS} "${INSTCONFDIR}\configure.bat" "set JAVA_HOME=" "$javaHome" $R0

    ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "jdk.home=" "$javaHome" $R0
    ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "jdk.majorversion=" "${JDK_MAJORVERSION}" $R0
    ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "jdk.version=" "${JDK_VERSION}" $R0

    ${if} ${SectionIsSelected} ${SecTomcat}
        DetailPrint "Configuring Tomcat"

        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "tomcat.dir=" "$tomcatDir" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "tomcat.httpPort=" "$tomcatHttpPort" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "tomcat.shutdownPort=" "$tomcatShutdownPort" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "tomcat.ajpPort=" "$tomcatAjpPort" $R0
        nsExec::ExecToLog '"${INSTCONFDIR}\configure.bat" configureTomcat'
        Pop $0

        DetailPrint "Ant returned $0"
    ${endIf}

    ${if} ${SectionIsSelected} ${SecWebClient}
        DetailPrint "Configuring WebClient"

        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "server.host=" "$platformServerHost" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "server.port=" "$platformServerPort" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "web.dir=" "$webClientDirectory" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "web.archive=" "$INSTDIR\${WEBCLIENT_WAR}" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "web.context=" "$webClientContext" $R0
        nsExec::ExecToLog '"${INSTCONFDIR}\configure.bat" configureWebClient'
        Pop $0

        DetailPrint "Ant returned $0"
    ${endIf}

    ${if} ${SectionIsSelected} ${SecServer}
        DetailPrint "Configuring server"
        DetailPrint "$INSTDIR\conf\settings.properties"
        ${ConfigWriteSE} "$INSTDIR\conf\settings.properties" "db.server=" "$pgHost:$pgPort" $R0
        ${ConfigWriteSE} "$INSTDIR\conf\settings.properties" "db.name=" "$pgDbName" $R0
        ${ConfigWriteSE} "$INSTDIR\conf\settings.properties" "db.user=" "$pgUser" $R0
        ${ConfigWriteSE} "$INSTDIR\conf\settings.properties" "db.password=" "$pgPassword" $R0
        ${ConfigWriteSE} "$INSTDIR\conf\settings.properties" "rmi.registryPort=" "$platformServerPort" $R0
        ${ConfigWriteSE} "$INSTDIR\conf\settings.properties" "rmi.registryPort=" "$platformServerPort" $R0
        ${ConfigWriteSE} "$INSTDIR\conf\settings.properties" "logics.initialAdminPassword=" "$platformServerPassword" $R0
    ${endIf}

    ${if} ${SectionIsSelected} ${SecIdea}
        DetailPrint "Configuring Intellij IDEA"

        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "server.archive=" "$INSTDIR\${SERVER_JAR}" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "lsfusion.library.name=" "${SERVER_LIBRARY_NAME}" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "idea.majorversion=" "${IDEA_MAJORVERSION}" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "db.host=" "$pgHost" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "db.port=" "$pgPort" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "db.user=" "$pgUser" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "db.pass=" "$pgPassword" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "idea.dir=" "$ideaDir" $R0
        ${ConfigWriteSE} "${INSTCONFDIR}\configure.properties" "idea.plugin=" "${IDEA_PLUGIN}" $R0
        nsExec::ExecToLog '"${INSTCONFDIR}\configure.bat" configureIdea'
        Pop $0

        DetailPrint "Ant returned $0"
    ${endIf}

    SetOutPath $INSTDIR
FunctionEnd

Function createServices
    ${if} ${SectionIsSelected} ${SecTomcat}
        ClearErrors
        DetailPrint "Installing Tomcat service"
        DetailPrint "VM DLL $jvmDll "
        nsExec::ExecToStack '"$tomcatDir\bin\tomcat7.exe" //IS//$tomcatServiceName --DisplayName "Apache Tomcat 7.0.47 $tomcatServiceName" --Description "Apache Tomcat 7 Server - http://tomcat.apache.org/" --LogPath "$tomcatDir\logs" --Install "$tomcatDir\bin\tomcat7.exe" --Jvm "$jvmDll" --StartPath "$tomcatDir" --StopPath "$tomcatDir"'
        Pop $0
        Pop $1
        ${ifNot} $0 == "0"
            DetailPrint $1
            MessageBox MB_OK|MB_ICONSTOP $(strErrorInstallingTomcatService)
        ${else}
            DetailPrint "Configuring $tomcatServiceName service"
    
            WriteRegStr HKLM "${REGKEY}" "tomcatServiceName" "$tomcatServiceName"
    
            nsExec::ExecToLog '"$tomcatDir\bin\tomcat7.exe" //US//$tomcatServiceName --Startup auto'
            nsExec::ExecToLog '"$tomcatDir\bin\tomcat7.exe" //US//$tomcatServiceName --Classpath "$tomcatDir\bin\bootstrap.jar;$tomcatDir\bin\tomcat-juli.jar" --StartClass org.apache.catalina.startup.Bootstrap --StopClass org.apache.catalina.startup.Bootstrap --StartParams start --StopParams stop --StartMode jvm --StopMode jvm'
            nsExec::ExecToLog '"$tomcatDir\bin\tomcat7.exe" //US//$tomcatServiceName --JvmOptions "-Dcatalina.home=$tomcatDir#-Dcatalina.base=$tomcatDir#-Djava.endorsed.dirs=$tomcatDir\endorsed#-Djava.io.tmpdir=$tomcatDir\temp#-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager#-Djava.util.logging.config.file=$tomcatDir\conf\logging.properties"'
            nsExec::ExecToLog '"$tomcatDir\bin\tomcat7.exe" //US//$tomcatServiceName --StdOutput auto --StdError auto'

            DetailPrint "Starting Tomcat service"
            nsExec::ExecToLog '"$tomcatDir\bin\tomcat7.exe" //ES//$tomcatServiceName'
        ${endIf}
    ${endIf}

    ${if} $createServices == "1"
        ${if} ${SectionIsSelected} ${SecServer}
            ClearErrors
            DetailPrint "Installing lsFusion Server service"
            nsExec::ExecToStack '"$INSTDIR\bin\lsfusion.exe" //IS//$platformServiceName --DisplayName "lsFusion Server" --Description "lsFusion server application http://lsfusion.ru" --LogPath "$INSTDIR\logs" --Install "$INSTDIR\bin\lsfusion.exe" --Jvm "$jvmDll" --StartPath "$INSTDIR" --StopPath "$INSTDIR"'
            Pop $0
            Pop $1
            ${ifNot} $0 == "0"
                DetailPrint $1
                MessageBox MB_OK|MB_ICONSTOP $(strErrorInstallingServerService)
            ${else}
                DetailPrint "Configuring $platformServiceName service"
    
                WriteRegStr HKLM "${REGKEY}" "platformServiceName" "$platformServiceName"
    
                nsExec::ExecToLog '"$INSTDIR\bin\lsfusion.exe" //US//$platformServiceName --Startup auto'
                nsExec::ExecToLog '"$INSTDIR\bin\lsfusion.exe" //US//$platformServiceName --Classpath "$INSTDIR\${SERVER_JAR};$INSTDIR\deploy-lib\*;$INSTDIR\deploy-class" --StartClass lsfusion.server.logics.BusinessLogicsBootstrap --StopClass lsfusion.server.logics.BusinessLogicsBootstrap --StartMethod start --StopMethod stop --StartMode jvm --StopMode jvm'
                nsExec::ExecToLog '"$INSTDIR\bin\lsfusion.exe" //US//$platformServiceName --JvmMs=512 --JvmMx=1024'
                nsExec::ExecToLog '"$INSTDIR\bin\lsfusion.exe" //US//$platformServiceName --StdOutput auto --StdError auto'
    
                DetailPrint "Starting lsFusion Server service"
                nsExec::ExecToLog '"$INSTDIR\bin\lsfusion.exe" //ES//$platformServiceName'
            ${endIf}
        ${endIf}
    ${endIf}
    
FunctionEnd

Function createShortcuts
    DetailPrint "Creating shortcuts"
    
    SetOutPath "$INSTDIR"

    ${if} ${SectionIsSelected} ${SecServer}
        ${if} $createServices == "1"
            CreateShortCut "$SMPROGRAMS\lsFusion Platform ${VERSION}\Start lsFusion Server.lnk" "$INSTDIR\bin\lsfusion.exe" "//ES//$platformServiceName" "$INSTDIR\resources\lsfusion.ico"
            CreateShortCut "$SMPROGRAMS\lsFusion Platform ${VERSION}\Stop lsFusion Server.lnk" "$INSTDIR\bin\lsfusion.exe" "//SS//$platformServiceName" "$INSTDIR\resources\lsfusion.ico"
        ${else}
            CreateShortCut "$SMPROGRAMS\lsFusion Platform ${VERSION}\Start lsFusion Server as console application.lnk" \
                            "$javaExe" \
                            "-Xms512m -Xmx1024m -cp ${SERVER_JAR};deploy-lib\*;deploy-class lsfusion.server.logics.BusinessLogicsBootstrap" \
                            "$INSTDIR\resources\lsfusion.ico"
        ${endIf}
    ${endIf}

    ${if} ${SectionIsSelected} ${SecClient}
        CreateShortCut "$SMPROGRAMS\lsFusion Platform ${VERSION}\lsFusion Client.lnk" \
                        "$javaHome\bin\javaw.exe" \
                        "-Xms512m -Xmx1024m -cp ${CLIENT_JAR} -Dlsfusion.client.hostname=$platformServerHost -Dlsfusion.client.hostport=$platformServerPort -Dlsfusion.client.exportname=default lsfusion.client.Main" \
                        "$INSTDIR\resources\lsfusion.ico"
        CreateShortCut "$DESKTOP\lsFusion Client.lnk" \
                        "$javaHome\bin\javaw.exe" \
                        "-Xms512m -Xmx1024m -cp ${CLIENT_JAR} -Dlsfusion.client.hostname=$platformServerHost -Dlsfusion.client.hostport=$platformServerPort -Dlsfusion.client.exportname=default lsfusion.client.Main" \
                        "$INSTDIR\resources\lsfusion.ico"
    ${endIf}

    ${if} ${SectionIsSelected} ${SecWebClient}
        CreateShortCut "$SMPROGRAMS\lsFusion Platform ${VERSION}\lsFusion Web Client.lnk" "http://127.0.0.1:$tomcatHttpPort/$webClientContext/" "" "$INSTDIR\resources\lsfusion.ico"
        CreateShortCut "$DESKTOP\lsFusion Web Client.lnk" "http://127.0.0.1:$tomcatHttpPort/$webClientContext/" "" "$INSTDIR\resources\lsfusion.ico"
    ${endIf}

    CreateShortCut "$SMPROGRAMS\lsFusion Platform ${VERSION}\Uninstall lsFusion Platform.lnk" "$INSTDIR\uninstall.exe"
    
    ${if} ${SectionIsSelected} ${SecIdea}
        SetOutPath "$ideaDir"
        CreateShortCut "$SMPROGRAMS\JetBrains\IntelliJ IDEA Community Edition ${IDEA_VERSION}.lnk" "$ideaDir\bin\${IDEA_EXE}" "" "$ideaDir\bin\${IDEA_EXE}"
        CreateShortCut "$DESKTOP\IntelliJ IDEA Community Edition ${IDEA_VERSION}.lnk" "$ideaDir\bin\${IDEA_EXE}" "" "$ideaDir\bin\${IDEA_EXE}"
    ${endIf}

FunctionEnd
