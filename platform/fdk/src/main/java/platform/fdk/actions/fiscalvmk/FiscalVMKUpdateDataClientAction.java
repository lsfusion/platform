package platform.fdk.actions.fiscalvmk;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalVMKUpdateDataClientAction implements ClientAction {

    int baudRate;
    int comPort;

    public FiscalVMKUpdateDataClientAction(Integer baudRate, Integer comPort) {
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        FiscalVMK.init();
        try {

            FiscalVMK.openPort(comPort, baudRate);
            FiscalVMK.closePort();

        } catch (RuntimeException e) {
            return FiscalVMK.getError(true);
        }
        return null;
    }
}
