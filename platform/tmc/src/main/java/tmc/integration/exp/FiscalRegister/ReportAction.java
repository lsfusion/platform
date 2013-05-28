package tmc.integration.exp.FiscalRegister;

import com.jacob.com.Dispatch;
import platform.interop.action.ExecuteClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class ReportAction extends ExecuteClientAction {
    public final static int XREPORT = 0;
    public final static int ZREPORT = 1;
    public final static int MONEY_BOX = 2;

    private final static int FONT = 0;
    int type;
    int comPort;

    public ReportAction(int type, int comPort) {
        this.type = type;
        this.comPort = comPort;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        Dispatch cashDispatch = FiscalReg.getDispatch(comPort);
        if (type == XREPORT) {
            Dispatch.call(cashDispatch, "XReport", FONT);
        } else if (type == ZREPORT) {
            Dispatch.call(cashDispatch, "ZReport", FONT);
        } else if (type == MONEY_BOX) {
            Dispatch.call(cashDispatch, "ExternalPulse", 1, 60, 10, 1);
        }
    }
}
