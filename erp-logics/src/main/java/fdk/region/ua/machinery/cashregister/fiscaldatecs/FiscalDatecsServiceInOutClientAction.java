package fdk.region.ua.machinery.cashregister.fiscaldatecs;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalDatecsServiceInOutClientAction implements ClientAction {

    int baudRate;
    int comPort;
    Double sum;

    public FiscalDatecsServiceInOutClientAction(Integer baudRate, Integer comPort, Double sum) {
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
        this.sum = sum;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        try {
            FiscalDatecs.init();

            FiscalDatecs.openPort(comPort, baudRate);

            if (!FiscalDatecs.inOut(sum))
                return "Недостаточно наличных в кассе";
            else {
                FiscalDatecs.closePort();
            }

        } catch (RuntimeException e) {
            return FiscalDatecs.getError();
        }
        return null;
    }
}
