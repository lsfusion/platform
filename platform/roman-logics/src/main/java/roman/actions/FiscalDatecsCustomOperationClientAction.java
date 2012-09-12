package roman.actions;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;


public class FiscalDatecsCustomOperationClientAction implements ClientAction {

    int type;

    public FiscalDatecsCustomOperationClientAction(int type) {
        this.type = type;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        //Dispatch cashDispatch = FiscalDatecs.getDispatch(comPort);
        PrintWriter zzz = new PrintWriter(new FileOutputStream(new File("D:/test/test_custom_file.txt")));
        try {
            //Dispatch.call(cashDispatch, "ShowError", true);
            zzz.println("Dispatch.call(cashDispatch, \"ShowError\")" + " " + true);

            String password = "0000";
            int lines = 10;
            switch (type) {
                case 1:
                    //Dispatch.call(cashDispatch, "XReport", password);
                    zzz.println("Dispatch.call(cashDispatch, \"XReport\")" + " " + password);
                    throw new RuntimeException("exception");
                    //break;
                case 2:
                    //Dispatch.call(cashDispatch, "getCurrentSums", 2);
                    zzz.println("Dispatch.call(cashDispatch, \"getCurrentSums\")" + " " + 2);
                    //Double VATSumSaleReceipt = Dispatch.call(cashDispatch, "s1") + Dispatch.call(cashDispatch, "s2") +
                    // Dispatch.call(cashDispatch, "s3") + Dispatch.call(cashDispatch, "s4") + Dispatch.call(cashDispatch, "s5");
                    Double VATSumSaleReceipt = 10.0;
                    //Dispatch.call(cashDispatch, "getCurrentSums", 3);
                    zzz.println("Dispatch.call(cashDispatch, \"getCurrentSums\")" + " " + 3);
                    //Double VATSumReturnReceipt = Dispatch.call(cashDispatch, "s1") + Dispatch.call(cashDispatch, "s2") +
                    // Dispatch.call(cashDispatch, "s3") + Dispatch.call(cashDispatch, "s4") + Dispatch.call(cashDispatch, "s5");
                    Double VATSumReturnReceipt = 3.0;

                    //Dispatch.call(cashDispatch, "ZReport", password);
                    zzz.println("Dispatch.call(cashDispatch, \"ZReport\")" + " " + password);
                    return new Double[]{VATSumSaleReceipt, VATSumReturnReceipt};
                    //break;
                case 3:
                    //Dispatch.call(cashDispatch, "AdvancePaper", lines);
                    zzz.println("Dispatch.call(cashDispatch, \"AdvancePaper\")" + " " + lines);
                    break;
                case 4:
                    //Dispatch.call(cashDispatch, "CutReceipt");
                    zzz.println("Dispatch.call(cashDispatch, \"CutReceipt\")");
                    break;
                case 5:
                    Date date = new Date(System.currentTimeMillis());
                    String dateFrom = fillZeros(date.getDate()) + fillZeros(date.getMonth()) + fillZeros(date.getYear());
                    String dateTo = fillZeros(date.getDate()) + fillZeros(date.getMonth()) + fillZeros(date.getYear());
                    //Dispatch.call(cashDispatch, "PrintTaxReport", password, dateFrom, dateTo);
                    zzz.println("Dispatch.call(cashDispatch, \"PrintTaxReport\")" + " " + password + " " + dateFrom + " " + dateTo);
                    break;
                default:
                    break;
            }
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

    private String fillZeros(int i) {
        String s = String.valueOf(i);
        while (s.length() < 2)
            s = "0" + s;
        return s;
    }
}
