!macro _LS_CreateLabel TEXT
    ${NSD_CreateLabel} 0 $0u 100% 14u "${TEXT}"
    IntOp $0 $0 + 17  
!macroend
!define LS_CreateLabel "!insertmacro _LS_CreateLabel"

!macro _LS_CreateControl TYPE LABEL VALUE_VAR CONTROL_VAR
    IntOp $5 $0 + 2  
    ${NSD_CreateLabel} 0 $5u 100u 14u "${LABEL}"
    ${NSD_Create${TYPE}} 120u $0u 100% 12u "${VALUE_VAR}"
    Pop ${CONTROL_VAR}
    IntOp $0 $0 + 17  
!macroend
!define LS_CreateControl "!insertmacro _LS_CreateControl"

!macro _LS_CreateNumber LABEL VALUE_VAR CONTROL_VAR
    ${LS_CreateControl} Number ${LABEL} ${VALUE_VAR} ${CONTROL_VAR}
!macroend
!define LS_CreateNumber "!insertmacro _LS_CreateNumber"

!macro _LS_CreateText LABEL VALUE_VAR CONTROL_VAR
    ${LS_CreateControl} Text ${LABEL} ${VALUE_VAR} ${CONTROL_VAR}
!macroend
!define LS_CreateText "!insertmacro _LS_CreateText"

!macro _LS_CreatePassword LABEL VALUE_VAR CONTROL_VAR
    ${LS_CreateControl} Password ${LABEL} ${VALUE_VAR} ${CONTROL_VAR}
!macroend
!define LS_CreatePassword "!insertmacro _LS_CreatePassword"

!macro _LS_CreateCheckBox LABEL CONTROL_VAR
    ${LS_CreateControl} CheckBox ${LABEL} "" ${CONTROL_VAR}
!macroend
!define LS_CreateCheckBox "!insertmacro _LS_CreateCheckBox"

!macro _LS_CreateDirRequest LABEL VALUE_VAR CONTROL_VAR ONBROWSE
    ${LS_CreateLabel} ${LABEL}
    
    ${NSD_CreateDirRequest} 0 $0u 280u 14u ${VALUE_VAR}
    Pop ${CONTROL_VAR}

    ${NSD_CreateBrowseButton} 282u $0u 15u 13u "..."
    Pop $R0
    ${NSD_OnClick} $R0 ${ONBROWSE}
    
    IntOp $0 $0 + 17
!macroend
!define LS_CreateDirRequest "!insertmacro _LS_CreateDirRequest"

!macro DefineOnBrowseFunction DIR_CONTROL ONBROWSE
    Function ${ONBROWSE}
        ${NSD_GetText} ${DIR_CONTROL} $R1
        nsDialogs::SelectFolderDialog "" "$R1"
        Pop $R1
    
        ${If} $R1 != "error"
            ${NSD_SetText} ${DIR_CONTROL} $R1
        ${EndIf}
    FunctionEnd
!macroend

!macro _LS_ConfigWriteSE FILE KEY VALUE RESULT
    ${StrRep} $0 ${VALUE} '\' '\\'
    ${ConfigWriteS} ${FILE} ${KEY} $0 ${RESULT}
!macroend
!define ConfigWriteSE "!insertmacro _LS_ConfigWriteSE"

!macro _LS_File FILE
    !ifndef SKIP_FILES
        File ${FILE}
    !endif
!macroend
!define SFile "!insertmacro _LS_File" 

!macro DisableSection SEC
  !insertmacro UnselectSection ${SEC}
  !insertmacro SetSectionFlag ${SEC} ${SF_RO}
!macroend

!macro HideSection SEC
  !insertmacro UnselectSection ${SEC}
  SectionSetText  ${SEC} ""
!macroend

!macro DefinePreFeatureFunction SEC NAME_PREFIX  
    Function ${NAME_PREFIX}PagePre
      ${IfNot} ${SectionIsSelected} ${SEC}
        Abort
      ${EndIf}
    FunctionEnd
!macroend

!macro CustomDirectoryPage ID HEADER TEXT_TOP TEXT_DESTINATION DIR_VAR PRE_FUNCTION
    !define MUI_PAGE_HEADER_SUBTEXT ${HEADER}
    !define MUI_DIRECTORYPAGE_VARIABLE ${DIR_VAR}
    !define MUI_DIRECTORYPAGE_TEXT_TOP ${TEXT_TOP}
    !define MUI_DIRECTORYPAGE_TEXT_DESTINATION ${TEXT_DESTINATION}
    !define MUI_PAGE_CUSTOMFUNCTION_PRE ${PRE_FUNCTION}
    !insertmacro MUI_PAGE_DIRECTORY
!macroend

;FileExists is already part of LogicLib, but returns true for directories as well as files
!macro _FileExists2 _a _b _t _f
    !insertmacro _LOGICLIB_TEMP
    StrCpy $_LOGICLIB_TEMP "0"
    StrCmp `${_b}` `` +4 0 ;if path is not blank, continue to next check
    IfFileExists `${_b}` `0` +3 ;if path exists, continue to next check (IfFileExists returns true if this is a directory)
    IfFileExists `${_b}\*.*` +2 0 ;if path is not a directory, continue to confirm exists
    StrCpy $_LOGICLIB_TEMP "1" ;file exists
    ;now we have a definitive value - the file exists or it does not
    StrCmp $_LOGICLIB_TEMP "1" `${_t}` `${_f}`
!macroend
!undef FileExists
!define FileExists `"" FileExists2`

!macro _DirExists _a _b _t _f
    !insertmacro _LOGICLIB_TEMP
    StrCpy $_LOGICLIB_TEMP "0"  
    StrCmp `${_b}` `` +3 0 ;if path is not blank, continue to next check
    IfFileExists `${_b}\*.*` 0 +2 ;if directory exists, continue to confirm exists
    StrCpy $_LOGICLIB_TEMP "1"
    StrCmp $_LOGICLIB_TEMP "1" `${_t}` `${_f}`
!macroend
!define DirExists `"" DirExists`


; Validates that a string name does not use any of the invalid
; characters: <>:"/\:|?*
; Note that space is also not permitted although it will be once
;
; Put the proposed service name on the stack
; If the name is valid, a 1 will be left on the stack
; If the name is invalid, a 0 will be left on the stack
Function validateMaybeEmptyNameString
  Pop $R0
  StrLen $R1 $R0
  StrCpy $R3 '<>:"/\:|?* '
  StrLen $R4 $R3

  loopInput:
    IntOp $R1 $R1 - 1
    IntCmp $R1 -1 valid
    loopTestChars:
      IntOp $R4 $R4 - 1
      IntCmp $R4 -1 loopTestCharsDone
      StrCpy $R2 $R0 1 $R1
      StrCpy $R5 $R3 1 $R4
      StrCmp $R2 $R5 invalid loopTestChars
    loopTestCharsDone:
    StrLen $R4 $R3
    Goto loopInput

  invalid:
  StrCpy $0 "0"
  Goto exit

  valid:
  StrCpy $0 "1"
  
  exit:
FunctionEnd

Function validateNameString
    Pop $R0
    ${if} $R0 == ""
        StrCpy $0 "0"
    ${else}
        Push $R0
        Call validateMaybeEmptyNameString
    ${endIf}
FunctionEnd
