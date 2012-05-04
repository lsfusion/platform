package tmc.integration.exp.FiscalRegister;

import com.jacob.com.Dispatch;
import platform.interop.action.ExecuteClientAction;
import platform.interop.action.ClientActionDispatcher;

import javax.swing.*;
import java.io.IOException;

public class MoneyOperationAction extends ExecuteClientAction {
    public final static int CASH_IN = 5;
    public final static int CASH_OUT = 6;
    private final static int FONT = 2;
    int type;
    double count;
    int comPort;

    public MoneyOperationAction(int type, int comPort, double count) {
        this.type = type;
        this.comPort = comPort;
        this.count = count;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        int n = JOptionPane.showConfirmDialog(
                null,
                "Вы действительно хотите выполнить эту операцию?",
                "Внесение/изъятие наличных",
                JOptionPane.YES_NO_OPTION);
        if (n != JOptionPane.YES_OPTION) {
            return;
        }

        Dispatch cashDispatch = FiscalReg.getDispatch(comPort);
        Dispatch.call(cashDispatch, "OpenFiscalDoc", type);

        try {

            int k = FiscalReg.printHeaderAndNumbers(cashDispatch, null);

            Dispatch.invoke(cashDispatch, "AddItem", Dispatch.Method, new Object[]{0, count, false,
                    0, 1, 0, 1, 1, 0, "шт.", FONT, 0, k++, 0}, new int[1]);

            Dispatch.call(cashDispatch, "AddTotal", FONT, 0, k++, 15);

            Dispatch.call(cashDispatch, "CloseFiscalDoc");
        } catch (RuntimeException e) {
            Dispatch.call(cashDispatch, "CancelFiscalDoc", false);
            throw e;
        }
        Dispatch.call(cashDispatch, "ExternalPulse", 1, 60, 10, 1);
    }
}
