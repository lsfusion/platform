package roman.actions.fiscaldatecs;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalDatecsDisplayTextClientAction implements ClientAction {

    int baudRate;
    int comPort;
    ReceiptItem receiptItem;

    public FiscalDatecsDisplayTextClientAction(Integer baudRate, Integer comPort, ReceiptItem receiptItem) {
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
        this.receiptItem = receiptItem;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        FiscalDatecs.init();
        try {

            FiscalDatecs.openPort(comPort, baudRate);

            FiscalDatecs.displayText(receiptItem);

            FiscalDatecs.closePort();

        } catch (RuntimeException e) {
            return FiscalDatecs.getError();
        }
        return null;
    }
}
