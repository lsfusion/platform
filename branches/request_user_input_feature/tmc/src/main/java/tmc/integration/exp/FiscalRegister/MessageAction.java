package tmc.integration.exp.FiscalRegister;

import platform.interop.action.ExecuteClientAction;
import platform.interop.action.ClientActionDispatcher;

import javax.swing.*;
import java.io.IOException;

public class MessageAction extends ExecuteClientAction {
    public final static int SERIAL_NUM = 0;
    public final static int LAST_DOC_NUM = 1;
    public final static int COUNTER = 2;
    int type;
    int port;


    public MessageAction(int type, int port) {
        this.type = type;
        this.port = port;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        String msg = null;
        String caption = null;

        if (type == SERIAL_NUM) {
            msg = FiscalReg.getInfo("SerialNumber", port, null);
            caption = "Серийный номер";
        } else if (type == LAST_DOC_NUM) {
            msg = FiscalReg.getInfo("LastRecNumber", port, "QueryLastDocInfo");
            caption = "Номер последнего чека";
        } else if (type == COUNTER) {
            msg = FiscalReg.getQuery(port, "QueryCounter");
            caption = "Денег в кассе";
        }
        
        JOptionPane.showMessageDialog(null, msg, caption, JOptionPane.INFORMATION_MESSAGE);
    }
}
