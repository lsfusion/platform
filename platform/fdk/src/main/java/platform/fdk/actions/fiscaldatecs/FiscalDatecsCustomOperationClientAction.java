package platform.fdk.actions.fiscaldatecs;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalDatecsCustomOperationClientAction implements ClientAction {

    int type;
    int baudRate;
    int comPort;

    public FiscalDatecsCustomOperationClientAction(int type, Integer baudRate, Integer comPort) {
        this.type = type;
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        try {
            FiscalDatecs.init();
            FiscalDatecs.openPort(comPort, baudRate);
            switch (type) {
                case 1:
                    FiscalDatecs.xReport();
                    break;
                case 2:
                    Double VATSumSaleZReport = FiscalDatecs.getCurrentSums(2);
                    Double VATSumReturnZReport = FiscalDatecs.getCurrentSums(3);
                    //FiscalDatecs.printTaxReport();
                    FiscalDatecs.zReport();
                    FiscalDatecs.delArticle(0);
                    FiscalDatecs.closePort();
                    return new Double[]{VATSumSaleZReport, VATSumReturnZReport};
                case 3:
                    FiscalDatecs.advancePaper(10);
                    break;
                case 4:
                    FiscalDatecs.cutReceipt();
                    break;
                case 5:
                    FiscalDatecs.cancelReceipt();
                    break;
                default:
                    break;
            }
            FiscalDatecs.closePort();
        } catch (RuntimeException e) {
            return FiscalDatecs.getError();
        }
        return null;
    }
}
