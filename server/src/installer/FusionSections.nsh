
;!define SKIP_FILES 1

# Installer sections
SubSection "!${PLATFORM_SECTION_NAME}" SecPlatform
    Section -CoreFiles
        DetailPrint "Begin installation"
        
        SetOutPath ${INSTBINDIR}
        SetOverwrite on
        
        ${SFile} install-bin\${ANT_ARCHIVE}
        
        DetailPrint "Extracting Ant to ${INSTBINDIR}"
        
        nsisunz::Unzip "${INSTBINDIR}\${ANT_ARCHIVE}" "${INSTBINDIR}"
        Pop $0
        StrCmp $0 "success" ok
          DetailPrint "$0" ;print error message to log
          Abort
        ok:

        Delete "${INSTBINDIR}\${ANT_ARCHIVE}"

        SetOutPath ${INSTCONFDIR}
        File "install-config\*.*"
        
        SetOutPath $INSTDIR
        File /r "conf"
        File /r "resources"
        
    SectionEnd

    Section "${SERVER_SECTION_NAME}" SecServer
        SetOutPath $INSTDIR
        SetOverwrite on
        
        File /r "deploy-lib"
        File /r "deploy-class"
        ${SFile} install-bin\${SERVER_JAR}

        SetOutPath $INSTDIR\bin
        File /oname=lsfusion.exe bin\lsfusion${ARCH}.exe

        WriteRegStr HKLM "${REGKEY}\Components" "${SERVER_SECTION_NAME}" 1
    SectionEnd

    Section "${CLIENT_SECTION_NAME}" SecClient
        SetOutPath $INSTDIR
        SetOverwrite on
        
        ${SFile} install-bin\${CLIENT_JAR}
        
        WriteRegStr HKLM "${REGKEY}\Components" "${SERVER_SECTION_NAME}" 1
    SectionEnd
    
    Section "${WEBCLIENT_SECTION_NAME}" SecWebClient
        SetOutPath $INSTDIR
        SetOverwrite on
        
        ${SFile} install-bin\${WEBCLIENT_WAR}
        
        WriteRegStr HKLM "${REGKEY}\Components" "${SERVER_SECTION_NAME}" 1
    SectionEnd
    
    Section "${MENU_SECTION_NAME}" SecShortcuts
;        SectionIn 1 2 3
    SectionEnd
    
    Section "${SERVICES_SECTION_NAME}" SecServices
;        SectionIn 1 2 3
    SectionEnd
SubSectionEnd

Section "${JAVA_SECTION_NAME}" SecJava
    SetOutPath ${INSTBINDIR}
    SetOverwrite on

    ; install Java if no recent is installed
    ${if} $javaVersion == ""
        ${SFile} install-bin\${JAVA_INSTALLER}

        ExecWait "${INSTBINDIR}\${JAVA_INSTALLER}" $0

        Call initJavaFromRegistry
        ${if} $javaHome == ""
            DetailPrint "JDK wasn't isntalled succesfully: can't find javaHome in registry. Try to install JDK manually and restart installer"
            Abort
        ${endIf}

        WriteRegStr HKLM "${REGKEY}\Components" "${JAVA_SECTION_NAME}" 1

        Delete "${INSTBINDIR}\${JAVA_INSTALLER}"
    ${else}
        DetailPrint "Skipping Java installation"
    ${endif}
SectionEnd

Section "${PG_SECTION_NAME}" SecPG
    SetOutPath ${INSTBINDIR}
    SetOverwrite on

    ; Install PostgreSQL if no recent version is installed
    ${if} $pgVersion == ""
        ${SFile} install-bin\${PG_INSTALLER}

        ExecWait '"${INSTBINDIR}\${PG_INSTALLER}" --mode unattended --unattendedmodeui none --prefix "$pgDir" --datadir "$pgDir\data" --superpassword "$pgPassword" --serverport $pgPort --servicename "$pgServiceName"' $0
        DetailPrint "PostgreSQL installation returned $0"
        
        WriteRegStr HKLM "${REGKEY}\Components" "${PG_SECTION_NAME}" 1
        WriteRegStr HKLM "${REGKEY}" "postgreInstallDir" "$pgDIr"

        Delete "${INSTBINDIR}\${PG_INSTALLER}"
    ${else}
        DetailPrint "Skipping PostgreSQL installation"
    ${endif}
SectionEnd

Section "${TOMCAT_SECTION_NAME}" SecTomcat
    SetOutPath ${INSTBINDIR}
    SetOverwrite on

    ; install Tomcat if no recent is installed
    ${if} $tomcatVersion == ""
        ${SFile} install-bin\${TOMCAT_ARCHIVE}
        
        DetailPrint "Extracting Tomcat to $tomcatDir"
        
        nsisunz::Unzip "${INSTBINDIR}\${TOMCAT_ARCHIVE}" "$tomcatDir"
        Pop $0
        StrCmp $0 "success" ok
          DetailPrint "$0" ;print error message to log
        ok:

        WriteRegStr HKLM "${REGKEY}\Components" "${TOMCAT_SECTION_NAME}" 1
        WriteRegStr HKLM "${REGKEY}" "tomcatInstallDir" "$tomcatDir"

        Delete "${INSTBINDIR}\${TOMCAT_ARCHIVE}"
    ${else}
        DetailPrint "Skipping Apache Tomcat installation"
    ${endif}
SectionEnd

# Section Descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SecPlatform} $(strPlatformSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecServer} $(strServerSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecClient} $(strClientSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecWebClient} $(strWebClientSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecShortcuts} $(strShortcutsSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecServices} $(strServicesSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecPG} $(strPgSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecJava} $(strJavaSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SecTomcat} $(strTomcatSectionDescription)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

!insertmacro DefinePreFeatureFunction ${SecTomcat} tomcat
;!insertmacro DefinePreFeatureFunction ${SecJava} java
!insertmacro DefinePreFeatureFunction ${SecPG} pg
