package roman.actions;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

import java.text.NumberFormat;

public class FiscalDatecs {
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

    static void init(int comPort, String reason) {
        dispose(reason);

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

    public static void dispose(String reason) {
        if (cashDispatch != null) {
            try {
                Dispatch.call(cashDispatch, "Close", false);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при закрытии соединения с фискальным регистратором\n" + reason, e);
            }
            cashDispatch = null;
            System.gc();
        }
    }

    public static Dispatch getDispatch(int comPort) {
        initDispatch(comPort, "Dispatch");
        return cashDispatch;
    }

    public static void initDispatch(int comPort, String type) {
        if (cashDispatch == null) {
            init(comPort, "Init : " + type);
        } else {
            try {
                Dispatch.call(cashDispatch, "TestConnection");
                if (!cashRegister.getProperty("Active").getBoolean()) {
                    init(comPort, "not Active : " + type);
                }
            } catch (Exception e) {
                init(comPort, "TestConnection : " + type + "\n" + e.toString());
            }
        }
    }

    public static String getFiscalString(String str) {
        return str.substring(0, Math.min(str.length(), WIDTH));
    }

    public static int printHeaderAndNumbers(Dispatch cashDispatch, ReceiptInstance receipt) {
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
        if (receipt != null) {
            /*if (receipt.cashierName != null) {
                Dispatch.call(cashDispatch, "AddCustom", getFiscalString("Продавец: " + receipt.cashierName), FONT, 0, k++);
            }*/

            /*if (receipt.clientName != null) {
                Dispatch.call(cashDispatch, "AddCustom", getFiscalString("Покупатель: " + receipt.clientName), FONT, 0, k++);

                if (receipt.clientSum != null) {
                    Dispatch.call(cashDispatch, "AddCustom", getFiscalString("Накопленная сумма: " + receipt.clientSum.intValue()), FONT, 0, k++);
                }
            }*/
        }
        Dispatch.call(cashDispatch, "AddCustom", delimetr, FONT, 0, k++);
        return k;
    }

    public static String getInfo(String property, int comPort, String query) {
        initDispatch(comPort, "Info");
        if (query != null) {
            Dispatch.call(cashDispatch, query);
        }
        Object result = cashRegister.getProperty(property);
        //Dispatch.call(cashDispatch, "Close", true);
        return result.toString();
    }

    public static String getQuery(int comPort, String query) {
        initDispatch(comPort, "Query");
        long result = Dispatch.call(cashDispatch, "QueryCounter", 11, false).getCurrency().longValue();
        result /= 10000;
        return NumberFormat.getInstance().format(result);
    }

}
