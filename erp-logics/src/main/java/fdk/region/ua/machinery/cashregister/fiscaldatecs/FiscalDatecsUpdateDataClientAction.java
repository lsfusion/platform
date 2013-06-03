package fdk.region.ua.machinery.cashregister.fiscaldatecs;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

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

            if(updateData.operatorList.isEmpty())
                updateData.operatorList.add(new UpdateDataOperator(1, "Кассир по умолчанию"));
            for (UpdateDataOperator operator : updateData.operatorList) {
                FiscalDatecs.setOperatorName(operator);
            }

            Double[] rates = new Double[4];
            for (UpdateDataTaxRate rate : updateData.taxRateList) {
                if(rate.taxRateNumber<=4)
                rates[rate.taxRateNumber - 1] = rate.taxRateValue;
            }
            String code = "";
            for (Double rate : rates) {
                code += (rate == null ? 0 : 1);
            }

            FiscalDatecs.setMulDecCurRF(code, rates);
            FiscalDatecs.closePort();

        } catch (RuntimeException e) {
            return FiscalDatecs.getError();
        }
        return null;
    }
}
