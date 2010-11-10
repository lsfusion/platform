package tmc.integration.exp.FiscalRegistar;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

import java.text.NumberFormat;

public class FiscalReg {
    final static int FONT = 2;
    final static int WIDTH = 28;
    static String delimetr = "";

    static {
        for (int i = 0; i < WIDTH; i++) {
            delimetr += "*";
        }
    }

    static Dispatch cashDispatch;
    static ActiveXComponent cashRegister;

    static void init(int comPort) {
        if (cashDispatch != null) {
            try {
                Dispatch.call(cashDispatch, "Close", true);
            }
            catch (Exception e) {
            }
        }
        cashRegister = new ActiveXComponent("Incotex.MercuryFPrtX");
        cashRegister.setProperty("PortNum", comPort);
        cashRegister.setProperty("BaudRate", 115200);
        cashRegister.setProperty("Password", "0000");

        cashDispatch = cashRegister.getObject();
        Dispatch.call(cashDispatch, "Open");
        Dispatch.call(cashDispatch, "SetDisplayBaudRate", 9600);
        try {
            Thread.sleep(100);
        }
        catch (Exception e) {
        }
    }


    public static Dispatch getDispatch(int comPort) {
        if (cashDispatch == null) {
            init(comPort);
        } else {
            try {
                Dispatch.call(cashDispatch, "TestConnection");
                if (!cashRegister.getProperty("Active").getBoolean()) {
                    init(comPort);
                }
            } catch (Exception e) {
                init(comPort);
            }
        }
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
        if (cashDispatch == null) {
            init(comPort);
        }
        if (query != null) {
            Dispatch.call(cashDispatch, query);
        }
        Object result = cashRegister.getProperty(property);
        //Dispatch.call(cashDispatch, "Close", true);
        return result.toString();
    }

    public static String getQuery(int comPort, String query) {
        if (cashDispatch == null) {
            init(comPort);
        }
        long result = Dispatch.call(cashDispatch, "QueryCounter", 11, false).getCurrency().longValue();
        result /= 10000;
        return NumberFormat.getInstance().format(result);
    }

}
