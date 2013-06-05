package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalVMKDisplayTextClientAction implements ClientAction {

    int baudRate;
    int comPort;
    ReceiptItem receiptItem;

    public FiscalVMKDisplayTextClientAction(Integer baudRate, Integer comPort, ReceiptItem receiptItem) {
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
        this.receiptItem = receiptItem;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        FiscalVMK.init();
        try {

            FiscalVMK.openPort(comPort, baudRate);

            FiscalVMK.displayText(receiptItem);

            FiscalVMK.closePort();

        } catch (RuntimeException e) {
            return FiscalVMK.getError(true);
        }
        return null;
    }
}
