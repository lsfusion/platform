package roman.actions;

import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.sun.corba.se.impl.orbutil.DenseIntMapImpl;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


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

        //Dispatch cashDispatch = FiscalDatecs.getDispatch(comPort);
        PrintWriter zzz = new PrintWriter(new FileOutputStream(new File("D:/test/test_update_file.txt")));
        try {

            //Dispatch.call(cashDispatch, "ShowError", true);
            zzz.println("Dispatch.call(cashDispatch, \"ShowError\")" + " " + true);

            //Dispatch.call(cashDispatch, "OpenPort", comPort, baudRate);
            zzz.println("Dispatch.call(cashDispatch, \"OpenPort\")" + " " + comPort + " " + baudRate);

            String password = "0000";
            for (UpdateDataOperator operator : updateData.operatorList) {
                //Dispatch.call(cashDispatch, "SetOperatorName", operator.operatorNumber, password, operator.operatorName);
                zzz.println("Dispatch.call(cashDispatch, \"SetOperatorName\")" + " " + operator.operatorNumber + " " + password + " " + operator.operatorName);
            }

            Double[] rates = new Double[4];
            for(UpdateDataTaxRate rate : updateData.taxRateList){
                rates[rate.taxRateNumber-1] = rate.taxRateValue;
            }
            String code = "";
            for(Double rate : rates){
                code += (rate==null ? 0 : 1);
            }

            //Dispatch.call(cashDispatch, "SetMulDecCurRF", "0000", 2, code, rates[0], rates[1], rates[2], rates[3]);
            zzz.println("Dispatch.call(cashDispatch, \"SetMulDecCurRF\")" + " " + "0000" + " " + 2 + " " + code + " " + rates[0] + " " + rates[1] + " " + rates[2] + " " + rates[3]);

            //Dispatch.call(cashDispatch, "ClosePort");
            zzz.println("Dispatch.call(cashDispatch, \"ClosePort\")");
            zzz.close();

        } catch (RuntimeException e) {
            //Variant lastError =  cashDispatch.call(cashDispatch, "lastError");
            //Variant lastErrorText =  cashDispatch.call(cashDispatch, "lastErrorText");
            String lastError = "-1";
            String lastErrorText =  "some error";
            zzz.println("cashDispatch.call(cashDispatch, \"lastError)\")" + " " + lastError);
            zzz.println("cashDispatch.call(cashDispatch, \"lastErrorText)\")" + " " + lastErrorText);
            zzz.close();
            return lastError;
            //throw e;
        }
        return null;
    }
}
