package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalVMKCustomOperationClientAction implements ClientAction {

    int type;
    int baudRate;
    int comPort;

    public FiscalVMKCustomOperationClientAction(int type, Integer baudRate, Integer comPort) {
        this.type = type;
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        try {
            FiscalVMK.init();
            FiscalVMK.openPort(comPort, baudRate);
            switch (type) {
                case 1:
                    FiscalVMK.xReport();
                    break;
                case 2:
                    FiscalVMK.opensmIfClose();
                    FiscalVMK.zReport();
                    FiscalVMK.closePort();
                    break;
                case 3:
                    FiscalVMK.advancePaper(3);
                    break;
                case 4:
                    FiscalVMK.cancelReceipt();
                    break;
                default:
                    break;
            }
            FiscalVMK.closePort();
        } catch (RuntimeException e) {
            return FiscalVMK.getError(true);
        }
        return null;
    }
}
