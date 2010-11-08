package tmc.integration.exp.FiscalRegistar;

import com.jacob.com.Dispatch;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class ReportAction implements ClientAction {
    public final static int XREPORT = 0;
    public final static int ZREPORT = 1;
    private final static int FONT = 2;
    int type;
    int comPort;

    public ReportAction(int type, int comPort) {
        this.type = type;
        this.comPort = comPort;
    }

    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        Dispatch cashDispatch = FiscalReg.createDispatch(comPort);
        try {
            if (type == XREPORT) {
                Dispatch.call(cashDispatch, "XReport", FONT);
            } else if(type == ZREPORT) {
                Dispatch.call(cashDispatch, "ZReport", FONT);
            }
        } catch (RuntimeException e) {
            throw e;
        } finally {
            Dispatch.call(cashDispatch, "Close", true);
        }
    }

    public boolean isBeforeApply() {
        return false;
    }
}
