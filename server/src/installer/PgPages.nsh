
Page custom pagePasswordPre pagePasswordLeave

Var Dialog
Var tfPassword1
Var tfPassword2
Var tfPort

Function pagePasswordPre
    CALL pgPagePre

    !insertmacro MUI_HEADER_TEXT $(strPostgreOptions) ""

    nsDialogs::Create /NOUNLOAD 1018

    Pop $Dialog

    ${If} $Dialog == error
        Abort
    ${EndIf}

    ${NSD_CreateLabel} 0 0 100% 12u $(strPasswordMessage)

    ${NSD_CreateLabel} 0u 20u 79u 12u $(strPasswordLabel)
    ${NSD_CreatePassword} 80u 18u 100% 12u ""
    Pop $tfPassword1

    ${NSD_CreateLabel} 0u 42u 79u 12u $(strPasswordRetypeLabel)
    ${NSD_CreatePassword} 80u 40u 100% 12u ""
    Pop $tfPassword2
    
    ${NSD_CreateLabel} 0u 64u 100% 12u $(strPortMessage)
    ${NSD_CreateLabel} 0u 84u 79u 12u $(strPortLabel)
    ${NSD_CreateNumber} 80u 82u 100% 12u ""
    Pop $tfPort

    ${NSD_SetText} $tfPassword1 $PG_PASSWORD
    ${NSD_SetText} $tfPassword2 $PG_PASSWORD
    ${NSD_SetText} $tfPort $PG_PORT

    nsDialogs::Show

FunctionEnd


Function pagePasswordLeave
    ${NSD_GetText} $tfPassword1 $9
    ${NSD_GetText} $tfPassword2 $8
    ${NSD_GetText} $tfPort $7
    ${if} $9 != $8
        MessageBox MB_OK|MB_ICONEXCLAMATION $(strNotIdenteicalPasswords)
        Abort
    ${endif}
    StrLen $0 $9
    ${if} $0 < 5
        MessageBox MB_OK|MB_ICONEXCLAMATION $(strPasswordTooShort)
        Abort
    ${endif}

    ${if} $7 < 0
    ${orIf} $7 > 65535
        MessageBox MB_OK|MB_ICONEXCLAMATION $(strInvalidPort)
        Abort
    ${endif}

    StrCpy $PG_PASSWORD $9
    StrCpy $PG_PORT $7

FunctionEnd
