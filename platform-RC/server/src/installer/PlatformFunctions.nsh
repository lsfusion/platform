
Var tfPlatformServerHost
Var tfPlatformServerPort
Var tfPlatformServerPassword1
Var tfPlatformServerPassword2
Var tfPlatformServiceName
Var tfWebClientContext
Var tfWebClientDirectory

Function platformConfigPagePre
    ${ifNot} ${SectionIsSelected} ${SecServer}
    ${andIfNot} ${SectionIsSelected} ${SecWebClient}
    ${andIfNot} $createShortcuts == "1"
        Abort
    ${endIf}

    !insertmacro MUI_HEADER_TEXT $(strPlatformOptions) ""

    nsDialogs::Create /NOUNLOAD 1018

    StrCpy $0 "0"
    
    ${ifNot} ${SectionIsSelected} ${SecServer}
        ${LS_CreateText} "$(strPlatformServerHost)" $platformServerHost $tfPlatformServerHost
    ${endIf}

    ${LS_CreateNumber} "$(strPlatformServerPort)" $platformServerPort $tfPlatformServerPort
    ${LS_CreateLabel} "$(strPlatformServerPasswordMessage)"
    ${LS_CreatePassword} "$(strPassword)" $platformServerPassword $tfPlatformServerPassword1
    ${if} ${SectionIsSelected} ${SecServer}
        ${LS_CreatePassword} "$(strPasswordRetype)" $platformServerPassword $tfPlatformServerPassword2

        ${if} $createServices == "1"
            ${LS_CreateText} "$(strServiceName)" $platformServiceName $tfPlatformServiceName
        ${endIf}
    ${endIf}

    ${if} ${SectionIsSelected} ${SecWebClient}
        ${LS_CreateLabel} "$(strWebClientContextMessage)"
        ${LS_CreateText} "$(strWebClientContext)" $webClientContext $tfWebClientContext
        
        ${ifNot} ${SectionIsSelected} ${SecTomcat}
            ${LS_CreateDirRequest2} $(strWebClientDirectory) $webClientDirectory $tfWebClientDirectory existingTomcatDir_onDirBrowse
        ${endIf}
    ${endIf}

    nsDialogs::Show
FunctionEnd

!insertmacro DefineOnBrowseFunction $tfWebClientDirectory existingTomcatDir_onDirBrowse

Function platformConfigPageLeave
    ${ifNot} ${SectionIsSelected} ${SecServer}
        ${NSD_GetText} $tfPlatformServerHost $platformServerHost
        Push $platformServerHost
        Call validateNameString
        ${if} $0 == "0"
            MessageBox MB_ICONEXCLAMATION|MB_OK $(strInvalidHostName)
            Abort
        ${endIf}
    ${else}
        StrCpy $platformServerHost "localhost"
    ${endIf}

    ${NSD_GetText} $tfPlatformServerPort $platformServerPort
    ${if} $platformServerPort < 1
    ${orIf} $platformServerPort > 65535
        MessageBox MB_OK|MB_ICONEXCLAMATION $(strInvalidPort)
        Abort
    ${endif}
    
    ${NSD_GetText} $tfPlatformServerPassword1 $9
    ${if} $9 == ""
        MessageBox MB_ICONEXCLAMATION|MB_YESNO $(strContinueOnEmptyPassword) IDYES yes
        Abort
        yes:
    ${endIf}

    ${if} ${SectionIsSelected} ${SecServer}
        ${NSD_GetText} $tfPlatformServerPassword2 $8
        ${if} $9 != $8
            MessageBox MB_OK|MB_ICONEXCLAMATION $(strNotIdenticalPasswords)
            Abort
        ${endif}
    
        ${if} $createServices == "1"
            Push $platformServiceName
            Call validateServiceName
            ${if} $0 == "0"
                MessageBox MB_ICONEXCLAMATION|MB_OK $(strInvalidServiceName)
                Abort
            ${endIf}
        ${endIf}
    ${endIf}
    
    StrCpy $platformServerPassword $9

    ${if} ${SectionIsSelected} ${SecWebClient}
        ${ifNot} ${SectionIsSelected} ${SecTomcat}
            ${NSD_GetText} $tfWebClientDirectory $0
            ${if} $0 == ""
            ${orIf} ${DirExists} $0
                StrCpy $webClientDirectory $0
            ${else}
                MessageBox MB_ICONEXCLAMATION|MB_OK $(strInvalidWebClientDirectory)
                Abort
            ${endIf}
        ${else}
                StrCpy $webClientDirectory "$tomcatDir\webapps"
        ${endIf}
    
        ${NSD_GetText} $tfWebClientContext $webClientContext

        Push $webClientContext
        Call validateMaybeEmptyNameString
        ${if} $0 == "0"
            MessageBox MB_ICONEXCLAMATION|MB_OK $(strInvalidWebClientContext)
            Abort
        ${endIf}
    ${endIf}
FunctionEnd
