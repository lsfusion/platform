Var tfTomcatShutdownPort
Var tfTomcatHttpPort
Var tfTomcatAjpPort
Var tfTomcatServiceName

Function tomcatConfigPagePre
    ${ifNot} ${SectionIsSelected} ${SecTomcat}
        Abort
    ${endIf}
    
    !insertmacro MUI_HEADER_TEXT "$(strTomcatOptions)" ""

    nsDialogs::Create 1018

    StrCpy $0 "0"
    
    ${LS_CreateNumber} $(strTomcatShutdownPort) $tomcatShutdownPort $tfTomcatShutdownPort
    ${LS_CreateNumber} $(strTomcatHttpPort) $tomcatHttpPort $tfTomcatHttpPort
    ${LS_CreateNumber} $(strTomcatAjpPort) $tomcatAjpPort $tfTomcatAjpPort

    ${if} $createServices == "1"
        ${LS_CreateText} $(strServiceName) $tomcatServiceName $tfTomcatServiceName
    ${endIf}

    nsDialogs::Show
FunctionEnd

Function tomcatConfigPageLeave
    ${NSD_GetText} $tfTomcatShutdownPort $tomcatShutdownPort
    ${NSD_GetText} $tfTomcatHttpPort $tomcatHttpPort
    ${NSD_GetText} $tfTomcatAjpPort $tomcatAjpPort

    ${if} $tomcatShutdownPort < 1
    ${orIf} $tomcatShutdownPort > 65535
        MessageBox MB_OK|MB_ICONEXCLAMATION $(strInvalidShutdownPort)
        Abort
    ${endif}

    ${if} $tomcatHttpPort < 1
    ${orIf} $tomcatHttpPort > 65535
        MessageBox MB_OK|MB_ICONEXCLAMATION $(strInvalidHttpPort)
        Abort
    ${endif}

    ${if} $tomcatAjpPort < 1
    ${orIf} $tomcatAjpPort > 65535
        MessageBox MB_OK|MB_ICONEXCLAMATION $(strInvalidAjpPort)
        Abort
    ${endif}

    ${if} $createServices == "1"
        ${NSD_GetText} $tfTomcatServiceName $tomcatServiceName
    
        Push $tomcatServiceName
        Call validateServiceName
        ${if} $0 == "0"
            MessageBox MB_ICONEXCLAMATION|MB_OK $(strInvalidServiceName)
            Abort
        ${endIf}
    ${endIf}
    
    StrCpy $webClientDirectory "$tomcatDir\webapps"
FunctionEnd
