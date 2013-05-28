package equ.api;

import java.io.Serializable;
import java.util.List;

public class TerminalDocumentTypeInfo implements Serializable {
    public String id;
    public String name;
    public String nameInHandbook1;
    public String idTerminalHandbookType1;
    public String idTerminalHandbookType2;
    public String nameInHandbook2;
    public List<LegalEntityInfo> legalEntityInfoList;

    public TerminalDocumentTypeInfo(String id, String name, String nameInHandbook1, String idTerminalHandbookType1,
                                    String nameInHandbook2, String idTerminalHandbookType2, List<LegalEntityInfo> legalEntityInfoList) {
        this.id = id;
        this.name = name;
        this.nameInHandbook1 = nameInHandbook1;
        this.idTerminalHandbookType1 = idTerminalHandbookType1;
        this.nameInHandbook2 = nameInHandbook2;
        this.idTerminalHandbookType2 = idTerminalHandbookType2;
        this.legalEntityInfoList = legalEntityInfoList;
    }
}
