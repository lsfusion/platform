# Macro for selecting uninstaller sections
!macro HideUnsection SECTION_NAME UNSECTION_ID
    ReadRegStr $0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    MessageBox MB_OK "${REGKEY}\Components ${SECTION_NAME} VAL $0"

    ${if} ${Errors}
    ${orIf} $0 == ""
        !insertmacro HideSection ${UNSECTION_ID}
    ${endIf}
!macroend

Var tst

# Uninstaller sections
Section "!un.${PLATFORM_SECTION_NAME}" UnSecPlatform
    SectionIn RO
SectionEnd

Section /o "un.${JAVA_SECTION_NAME}" UnSecJava
    SectionIn RO
SectionEnd

Section /o "un.${PG_SECTION_NAME}" UnSecPG
SectionEnd

Section /o "un.${IDEA_SECTION_NAME}" UnSecIdea
SectionEnd

Section "un.${TOMCAT_SECTION_NAME}" UnSecTomcat
    SectionIn RO
SectionEnd

Section -un.Uninstall
    ; has to be first string by spec
    Delete /REBOOTOK $INSTDIR\uninstall.exe

    ${if} ${FileExists} "$INSTDIR\bin\lsfusion.exe"
        DetailPrint "Removing lsFusion Server service"
        ReadRegStr $0 HKLM "${REGKEY}" "platformServiceName"
        ${ifNot} ${Errors}
        ${andIfNot} $0 == ""
            nsExec::ExecToLog '"$INSTDIR\bin\lsfusion.exe" //DS//$0'
        ${else}
            DetailPrint "Can't find lsFusion Server service information in registry"
        ${endIf}
    ${endIf}


    ReadRegStr $0 HKLM "${REGKEY}" "tomcatInstallDir"
    ${ifNot} ${Errors}
    ${andIfNot} $0 == ""
        DetailPrint "Removing Apache Tomcat"
        
        DetailPrint "Removing tomcat service"
        ReadRegStr $1 HKLM "${REGKEY}" "tomcatServiceName"
        ${ifNot} ${Errors}
        ${andIfNot} $1 == ""
            nsExec::ExecToLog '"$0\bin\tomcat7.exe" //DS//$1'
        ${else}
            DetailPrint "Can't find Apache Tomcat service information in registry"
        ${endIf}

        DetailPrint "Removing tomcat directory"
        RMDir /r $0
    ${endIf}
    
    ${if} ${SectionIsSelected} ${UnSecPG}
        ReadRegStr $0 HKLM "${REGKEY}" "postgreInstallDir"
        ${ifNot} ${Errors}
        ${andIfNot} $0 == ""
        ${andIf} ${FileExists} "$0\uninstall-postgresql.exe"
            DetailPrint "Removing PostgreSQL"
            nsExec::ExecToLog '"$0\uninstall-postgresql.exe" --mode unattended'
        ${endIf}
    ${endIf}
        
    ${if} ${SectionIsSelected} ${UnSecIdea}
        ReadRegStr $0 HKLM "${REGKEY}" "ideaInstallDir"
        ${ifNot} ${Errors}
        ${andIfNot} $0 == ""
        ${andIf} ${FileExists} "$0\bin\Uninstall.exe"
            DetailPrint "Removing Intellij IDEA"
            nsExec::ExecToLog "$0\bin\Uninstall.exe /S"
            
            Delete "$DESKTOP\IntelliJ IDEA Community Edition ${IDEA_VERSION}.lnk"
            Delete "$SMPROGRAMS\JetBrains\IntelliJ IDEA Community Edition ${IDEA_VERSION}.lnk"
        ${endIf}
    ${endIf}
        
    DetailPrint "Cleaning registry"
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    DeleteRegKey HKLM "${REGKEY}"

    DetailPrint "Removing shortcuts"
    Delete "$DESKTOP\lsFusion Web Client.lnk"
    Delete "$DESKTOP\lsFusion Client.lnk"
    RMDir /r "$SMPROGRAMS\lsFusion Platform ${VERSION}"


    DetailPrint "Removing program directory"
    RmDir /r $INSTDIR
SectionEnd

# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path

    MessageBox MB_OK "INSTDIR $INSTDIR"
    
    !insertmacro MUI_UNGETLANGUAGE

    ReadRegStr $0 HKLM "${REGKEY}\Components" "${PG_SECTION_NAME}"
    MessageBox MB_OK "READ $0"

    ReadRegStr $0 HKLM "${REGKEY}\Components" "${JAVA_SECTION_NAME}"
    MessageBox MB_OK "READ2 $0"

    ReadRegStr $0 HKLM "${REGKEY}\Components" ${JAVA_SECTION_NAME}
    MessageBox MB_OK "READ3 $0"

    ReadRegStr $0 HKLM "${REGKEY}\Components" JDK3
    MessageBox MB_OK "READ4 $0"

    ReadRegStr $tst HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\1.7.0_75" "RuntimeLib"
    MessageBox MB_OK "READ5 $tst"

    ReadRegStr $0 HKLM "${REGKEY}\Components" "PostgreSQL 9.4"
    MessageBox MB_OK "READ5 $0"

    !insertmacro HideUnsection "${PG_SECTION_NAME}" ${UnSecPG}
    !insertmacro HideUnsection "${IDEA_SECTION_NAME}" ${UnSecIdea}
    !insertmacro HideUnsection "${JAVA_SECTION_NAME}" ${UnSecJava}
    !insertmacro HideUnsection "${TOMCAT_SECTION_NAME}" ${UnSecTomcat}
FunctionEnd

# Section Descriptions
!insertmacro MUI_UNFUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${UnSecPlatform} $(strPlatformUnSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${UnSecPG} $(strPgUnSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${UnSecIdea} $(strIdeaUnSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${UnSecJava} $(strJavaUnSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${UnSecTomcat} $(strTomcatUnSectionDescription)
!insertmacro MUI_UNFUNCTION_DESCRIPTION_END
