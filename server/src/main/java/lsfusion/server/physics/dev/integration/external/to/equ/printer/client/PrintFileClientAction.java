package lsfusion.server.physics.dev.integration.external.to.equ.printer.client;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;
import lsfusion.server.physics.dev.integration.external.to.equ.printer.PrintUtils;

import java.io.IOException;

public class PrintFileClientAction extends ExecuteClientAction {
    private RawFileData fileData;
    private String filePath;
    private String printerName;
    private String trayName;
    private boolean duplex;

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