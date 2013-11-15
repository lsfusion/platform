Name "LSFusion Platform"

SetCompressor lzma

# General Symbol Definitions
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION 1.2.0
!define COMPANY luxsoft
!define URL lsfusion.ru

!define FUSION_SECTION_NAME "LSFusion Platform"
!define PG_SECTION_NAME "PostgreSQL 9.2"
!define JDK_SECTION_NAME "JDK 1.7.0_45"
!define TOMCAT_SECTION_NAME "Apache Tomcat 7.0.47"


!define JDK_DEFAULT_DIR "$ProgramFiles${ARCH}\Java\jdk1.7.0_45"
!define PG_DEFAULT_DIR "$ProgramFiles${ARCH}\PostgreSQL\9.2"
!define TOMCAT_DEFAULT_DIR "$ProgramFiles${ARCH}\apache-tomcat-7.0.47"

# MUI Symbol Definitions
!define MUI_ICON lsfusion.ico
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_UNICON lsfusion.ico
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_LANGDLL_REGISTRY_ROOT HKLM
!define MUI_LANGDLL_REGISTRY_KEY ${REGKEY}
!define MUI_LANGDLL_REGISTRY_VALUENAME InstallerLanguage
; !define MUI_LANGDLL_ALWAYSSHOW 1

# Included files
!include Sections.nsh
!include MUI2.nsh
!include LogicLib.nsh
!include TextFunc.nsh
!include WordFunc.nsh
!include Utils.nsh
!include zipdll.nsh

# Needed Variables
Var JDK_VERSION     ; Version of the installed JDK
Var JDK_DIR         ; Directory of the installed JDK (empty string if no JDK)

Var PG_VERSION      ; Version of an installed PG
Var PG_DIR          ; Directory to install PostgreSQL
Var PG_PASSWORD     ; Password for PostgreSQL installtion (and all the rest for now)
Var PG_PORT         ; PostgreSQL Port (5432)

VAR TOMCAT_VERSION
Var TOMCAT_DIR

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE license.txt
!insertmacro MUI_PAGE_COMPONENTS

!define MUI_PAGE_HEADER_SUBTEXT $(strPostgreDirHeader)
!define MUI_DIRECTORYPAGE_VARIABLE $PG_DIR
!define MUI_DIRECTORYPAGE_TEXT_TOP $(strPostgreDirTextTop)
!define MUI_DIRECTORYPAGE_TEXT_DESTINATION $(strDestinationFolder)
!define MUI_PAGE_CUSTOMFUNCTION_PRE pgPagePre
!insertmacro MUI_PAGE_DIRECTORY

!include PgPages.nsh

!define MUI_PAGE_HEADER_SUBTEXT $(strJdkDirHeader)
!define MUI_DIRECTORYPAGE_VARIABLE $JDK_DIR
!define MUI_DIRECTORYPAGE_TEXT_TOP $(strJdkDirTextTop)
!define MUI_DIRECTORYPAGE_TEXT_DESTINATION $(strDestinationFolder)
!define MUI_PAGE_CUSTOMFUNCTION_PRE jdkPagePre
!insertmacro MUI_PAGE_DIRECTORY

!define MUI_PAGE_HEADER_SUBTEXT $(strTomcatDirHeader)
!define MUI_DIRECTORYPAGE_VARIABLE $TOMCAT_DIR
!define MUI_DIRECTORYPAGE_TEXT_TOP $(strTomcatDirTextTop)
!define MUI_DIRECTORYPAGE_TEXT_DESTINATION $(strDestinationFolder)
!define MUI_PAGE_CUSTOMFUNCTION_PRE tomcatPagePre
!insertmacro MUI_PAGE_DIRECTORY

!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

# Uninstaller pages
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English
!insertmacro MUI_LANGUAGE Russian

# Reserved Files
!insertmacro MUI_RESERVEFILE_LANGDLL
ReserveFile "${NSISDIR}\Plugins\AdvSplash.dll"

# Installer attributes
OutFile ${OUT_FILE}
InstallDir "$ProgramFiles${ARCH}\LSFusion Platform"
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion 1.2.0.0
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName "LSFusion Platform"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription ""
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright ""
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# Installer sections
Section "!${FUSION_SECTION_NAME}" SEC_FUSION
    SectionIn RO
    SetOutPath $INSTDIR
    SetOverwrite on
    
    File FusionBinaries\lsfusion-server-1.2.0.jar
    WriteRegStr HKLM "${REGKEY}\Components" "${FUSION_SECTION_NAME}" 1
SectionEnd

Section "${PG_SECTION_NAME}" SEC_PG
    SetOutPath $INSTDIR
    SetOverwrite on

    ; Install PostgreSQL if no recent version is installed
    ${if} $PG_VERSION == ""
        File ExternalBinaries\${PG_INSTALLER}

        ExecWait '"$INSTDIR\${PG_INSTALLER}" --mode unattended --unattendedmodeui none --prefix "$PG_DIR" --datadir "$PG_DIR\data" --superpassword "$PG_PASSWORD" --serverport $PG_PORT'
        
        WriteRegStr HKLM "${REGKEY}\Components" "${PG_SECTION_NAME}" 1

;        Delete "$INSTDIR\${PG_INSTALLER}"
    ${else}
        DetailPrint "Skipping PostgreSQL installation"
    ${endif}
SectionEnd

Section "${JDK_SECTION_NAME}" SEC_JDK
    SetOutPath $INSTDIR
    SetOverwrite on

    ; install JDK if no recent is installed
    ${if} $JDK_VERSION == ""
        File ExternalBinaries\${JDK_INSTALLER}

        ExecWait '"$INSTDIR\${JDK_INSTALLER}" /s ADDLOCAL="ToolsFeature,SourceFeature,PublicjreFeature" INSTALLDIR="$JDK_DIR"'

        WriteRegStr HKLM "${REGKEY}\Components" "${JDK_SECTION_NAME}" 1

;        Delete "$INSTDIR\${JDK_INSTALLER}"
    ${else}
        DetailPrint "Skipping JDK installation"
    ${endif}
SectionEnd

Section "${TOMCAT_SECTION_NAME}" SEC_TOMCAT
    SetOutPath $INSTDIR
    SetOverwrite on

    ; install Tomcat if no recent is installed
    ${if} $TOMCAT_VERSION == ""
        File ExternalBinaries\${TOMCAT_ARCHIVE}
        
        !insertmacro ZIPDLL_EXTRACT "$INSTDIR\${TOMCAT_ARCHIVE}" "$TOMCAT_DIR" "<ALL>"

        WriteRegStr HKLM "${REGKEY}\Components" "${TOMCAT_SECTION_NAME}" 1

;        Delete "$INSTDIR\${TOMCAT_ARCHIVE}"
    ${else}
        DetailPrint "Skipping Apache Tomcat installation"
    ${endif}
SectionEnd

Section -post SEC_POST
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
SectionEnd

# Uninstaller sections
Section /o "-un.${JDK_SECTION_NAME}" UNSEC_JDK
    Delete /REBOOTOK $INSTDIR\${JDK_INSTALLER}
    DeleteRegValue HKLM "${REGKEY}\Components" "${JDK_SECTION_NAME}"
SectionEnd

Section /o "-un.${PG_SECTION_NAME}" UNSEC_PG
    Delete /REBOOTOK $INSTDIR\${PG_INSTALLER}
    DeleteRegValue HKLM "${REGKEY}\Components" "${PG_SECTION_NAME}"
SectionEnd

Section /o "-un.${TOMCAT_SECTION_NAME}" UNSEC_TOMCAT
    Delete /REBOOTOK $INSTDIR\${TOMCAT_ARCHIVE}
    DeleteRegValue HKLM "${REGKEY}\Components" "${TOMCAT_SECTION_NAME}"
SectionEnd

Section /o "-un.${FUSION_SECTION_NAME}" UNSEC_FUSION
    RmDir /r /REBOOTOK $INSTDIR
    DeleteRegValue HKLM "${REGKEY}\Components" "${FUSION_SECTION_NAME}"
SectionEnd

Section -un.post UNSEC_POST
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
    RmDir /REBOOTOK $INSTDIR
SectionEnd

# Section Descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SEC_FUSION} $(strFusionSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SEC_PG} $(strPgSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SEC_JDK} $(strJdkSectionDescription)
!insertmacro MUI_DESCRIPTION_TEXT ${SEC_TOMCAT} $(strTomcatSectionDescription)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

# Installer functions
Function .onInit
    SetRegView ${ARCH}
    
    InitPluginsDir

    Push $R1
    File /oname=$PLUGINSDIR\spltmp.bmp lsfusion.bmp
    advsplash::show 1000 600 400 -1 $PLUGINSDIR\spltmp
    Pop $R1
    Pop $R1
    
    StrCpy $PG_DIR "${PG_DEFAULT_DIR}"
    StrCpy $JDK_DIR ${JDK_DEFAULT_DIR}
    StrCpy $TOMCAT_DIR ${TOMCAT_DEFAULT_DIR}
    
    !insertmacro MUI_LANGDLL_DISPLAY

    ; Check if JDK is installed
    ReadRegStr $JDK_VERSION HKLM "Software\JavaSoft\Java Development Kit" "CurrentVersion"
    ${VersionCompare} $JDK_VERSION "1.7" $0
    ; if installed JDK is outdated then install the new one.
    ${if} $0 == "2"
       StrCpy $JDK_VERSION ""
    ${else}
        !insertmacro DisableSection ${SEC_JDK}
    ${endif}

    ; Check if Tomcat is installed
    EnumRegKey $1 HKLM "Software\Apache Software Foundation\Tomcat\" "0"
    ${if} $1 == "7.0"
        !insertmacro DisableSection ${SEC_TOMCAT}
    ${else}
        StrCpy $TOMCAT_VERSION ""
    ${endif}
    
    ; Set defaults for PG installation
    StrCpy $PG_PASSWORD ""
    StrCpy $PG_PORT "5432"
    
    ; Check if PostgreSQL is installed
    EnumRegKey $1 HKLM "Software\PostgreSQL\Installations\" "0"
    ${if} $1 != ""
        ReadRegStr $PG_VERSION HKLM "SOFTWARE\PostgreSQL\Installations\$1" "Version"
        ReadRegStr $PG_DIR HKLM "SOFTWARE\PostgreSQL\Installations\$1" "Base Directory"
        ; if installed version is < 9.2 then abort
        ${VersionCompare} $PG_VERSION "9.2" $0
        ${if} $0 == "2"
            MessageBox MB_ICONEXCLAMATION|MB_OK $(strOldPostgreMessage)
            Abort
        ${else}
            !insertmacro DisableSection ${SEC_PG}
        ${endif}
    ${endif}
FunctionEnd

# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    !insertmacro MUI_UNGETLANGUAGE
    !insertmacro SelectUnsection "${FUSION_SECTION_NAME}" ${UNSEC_FUSION}
    !insertmacro SelectUnsection "${PG_SECTION_NAME}" ${UNSEC_PG}
    !insertmacro SelectUnsection "${JDK_SECTION_NAME}" ${UNSEC_JDK}
    !insertmacro SelectUnsection "${TOMCAT_SECTION_NAME}" ${UNSEC_TOMCAT}
FunctionEnd

# Custom functions
Function jdkPagePre
  ${IfNot} ${SectionIsSelected} ${SEC_JDK}
    Abort
  ${EndIf}
FunctionEnd

Function pgPagePre
  ${IfNot} ${SectionIsSelected} ${SEC_PG}
    Abort
  ${EndIf}
FunctionEnd

Function tomcatPagePre
  ${IfNot} ${SectionIsSelected} ${SEC_TOMCAT}
    Abort
  ${EndIf}
FunctionEnd

!include LocalizedString.nsh

