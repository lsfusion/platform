package roman.actions.fiscaldatecs;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class FiscalDatecsUpdateDataClientAction implements ClientAction {

    int baudRate;
    int comPort;
    UpdateDataInstance updateData;

    public FiscalDatecsUpdateDataClientAction(Integer baudRate, Integer comPort, UpdateDataInstance updateData) {
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
        this.updateData = updateData;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        FiscalDatecs.init();
        try {

            FiscalDatecs.openPort(comPort, baudRate);

            for (UpdateDataOperator operator : updateData.operatorList) {
                FiscalDatecs.setOperatorName(operator);
            }

            Double[] rates = new Double[4];
            for (UpdateDataTaxRate rate : updateData.taxRateList) {
                rates[rate.taxRateNumber - 1] = rate.taxRateValue;
            }
            String code = "";
            for (Double rate : rates) {
                code += (rate == null ? 0 : 1);
            }

            FiscalDatecs.setMulDecCurRF(code, rates);
            FiscalDatecs.closePort();
            FiscalDatecs.closeWriter();

        } catch (RuntimeException e) {
            return FiscalDatecs.getError();
        }
        return null;
    }
}
