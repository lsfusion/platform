package roman.actions.fiscaldatecs;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalDatecsCustomOperationClientAction implements ClientAction {

    int type;

    public FiscalDatecsCustomOperationClientAction(int type) {
        this.type = type;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        try {
            FiscalDatecs.init();
            switch (type) {
                case 1:
                    FiscalDatecs.xReport();
                    break;
                case 2:
                    Double VATSumSaleReceipt = FiscalDatecs.getCurrentSums(2);
                    Double VATSumReturnReceipt = FiscalDatecs.getCurrentSums(3);
                    FiscalDatecs.zReport();
                    FiscalDatecs.closeWriter();
                    return new Double[]{VATSumSaleReceipt, VATSumReturnReceipt};
                case 3:
                    FiscalDatecs.advancePaper(10);
                    break;
                case 4:
                    FiscalDatecs.cutReceipt();
                    break;
                case 5:
                    FiscalDatecs.printTaxReport();
                    break;
                default:
                    break;
            }
            FiscalDatecs.closeWriter();
        } catch (RuntimeException e) {
            return FiscalDatecs.getError();
        }
        return null;
    }
}
