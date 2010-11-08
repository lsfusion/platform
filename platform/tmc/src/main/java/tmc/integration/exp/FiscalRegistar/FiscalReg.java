package tmc.integration.exp.FiscalRegistar;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

public class FiscalReg {
    final static int FONT = 2;
    final static int WIDTH = 28;
    static String delimetr = "";

    static {
        for (int i = 0; i < WIDTH; i++) {
            delimetr += "*";
        }
    }


    public static Dispatch createDispatch(int comPort) {
        ActiveXComponent cashRegister = new ActiveXComponent("Incotex.MercuryFPrtX");

        cashRegister.setProperty("PortNum", comPort);
        cashRegister.setProperty("BaudRate", 115200);
        cashRegister.setProperty("Password", "0000");

        Dispatch cashDispatch = cashRegister.getObject();
        Dispatch.call(cashDispatch, "Open");
        return cashDispatch;
    }

    public static int printHeaderAndNumbers(Dispatch cashDispatch) {
        int k = 0;
        //печать заголовка
        Dispatch.call(cashDispatch, "AddHeaderLine", 1, FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddHeaderLine", 2, FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddHeaderLine", 3, FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddHeaderLine", 4, FONT, 0, k++);

        //печать номеров и даты
        Dispatch.call(cashDispatch, "AddSerialNumber", FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddTaxPayerNumber", FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddDateTime", FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddDocNumber", FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddReceiptNumber", FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddOperInfo", 0, FONT, 0, k++);
        Dispatch.call(cashDispatch, "AddCustom", delimetr, FONT, 0, k++);
        return k;
    }

    public static String getInfo(String property, int comPort, String query) {
        ActiveXComponent cashRegister = new ActiveXComponent("Incotex.MercuryFPrtX");

        cashRegister.setProperty("PortNum", comPort);
        cashRegister.setProperty("BaudRate", 115200);
        cashRegister.setProperty("Password", "0000");

        Dispatch cashDispatch = cashRegister.getObject();
        Dispatch.call(cashDispatch, "Open");
        if (query != null) {
            Dispatch.call(cashDispatch, query);
        }
        Object result = cashRegister.getProperty(property);
        Dispatch.call(cashDispatch, "Close", true);
        return result.toString();
    }

}
