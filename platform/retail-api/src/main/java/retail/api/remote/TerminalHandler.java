package retail.api.remote;

import java.io.IOException;
import java.util.List;

public abstract class TerminalHandler<S extends SalesBatch> extends MachineryHandler<TransactionTerminalInfo, TerminalInfo, S> {

    public abstract void sendTerminalDocumentTypes(List<TerminalInfo> terminalInfoList,
                                                   List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList) throws IOException;
}
