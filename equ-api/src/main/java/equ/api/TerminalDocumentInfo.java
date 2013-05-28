package equ.api;

import java.io.Serializable;
import java.util.List;

public class TerminalDocumentInfo implements Serializable {
    public Integer idDocument;
    public String typeDocument;
    public Integer handbook1;
    public Integer handbook2;
    public String title;
    public Double quantity;
    public List<TerminalDocumentDetailInfo> terminalDocumentDetailInfoList;

    public TerminalDocumentInfo(Integer idDocument, String typeDocument, Integer handbook1, Integer handbook2, String title,
                                Double quantity, List<TerminalDocumentDetailInfo> terminalDocumentDetailInfoList) {
        this.idDocument = idDocument;
        this.typeDocument = typeDocument;
        this.handbook1 = handbook1;
        this.handbook2 = handbook2;
        this.title = title;
        this.quantity = quantity;
        this.terminalDocumentDetailInfoList = terminalDocumentDetailInfoList;
    }
}
