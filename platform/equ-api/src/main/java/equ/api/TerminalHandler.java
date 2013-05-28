package equ.api;

import java.io.IOException;
import java.util.List;

public abstract class TerminalHandler<S extends SalesBatch> extends MachineryHandler<TransactionTerminalInfo, TerminalInfo, S> {

    public abstract void sendTerminalDocumentTypes(List<TerminalInfo> terminalInfoList,
                                                   List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList) throws IOException;

    public abstract List<TerminalDocumentInfo> readTerminalDocumentInfo(List<TerminalInfo> terminalInfoList) throws IOException;

    public abstract void finishSendingTerminalDocumentInfo(List<TerminalInfo> terminalInfoList,
                                                           List<TerminalDocumentInfo> terminalDocumentInfoList) throws IOException;

}
