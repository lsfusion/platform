package lsfusion.base.printer;

import lsfusion.base.PrintUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.IOException;

public class PrintFileClientAction extends ExecuteClientAction {
    public final RawFileData fileData;
    public final String filePath;
    public final String printerName;
    public final String trayName;
    public final boolean duplex;

    public PrintFileClientAction(RawFileData fileData, String filePath, String printerName, String trayName, boolean duplex) {
        this.fileData = fileData;
        this.filePath = filePath;
        this.printerName = printerName;
        this.trayName = trayName;
        this.duplex = duplex;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        PrintUtils.printFile(fileData, filePath, printerName, trayName, duplex);
    }
}