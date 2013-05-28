package tmc.integration.exp.FiscalRegister;

import com.jacob.com.Dispatch;
import platform.interop.action.ExecuteClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class NonFiscalPrintAction extends ExecuteClientAction {
    String msg;
    int comPort;

    public NonFiscalPrintAction(String msg, int comPort) {
        this.msg = msg;
        this.comPort = comPort;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        Dispatch cashDispatch = FiscalReg.getDispatch(comPort);
        Dispatch.call(cashDispatch, "PrintNonFiscal", msg, true, true);
        Dispatch.call(cashDispatch, "FeedAndCut", 4, true);
    }
}
