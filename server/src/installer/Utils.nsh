# Macro for selecting uninstaller sections
!macro SelectUnsection SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

!macro DisableSection SECTION
  !insertmacro UnselectSection ${SECTION}
  !insertmacro SetSectionFlag ${SECTION} ${SF_RO}
!macroend

!macro HideSection SECTION
  !insertmacro UnselectSection ${SECTION}
  SectionSetText  ${SECTION} ""
!macroend

