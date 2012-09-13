package roman.actions.fiscaldatecs;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;

public class FiscalDatecs {

    static Dispatch cashDispatch;
    static ActiveXComponent cashRegister;
    private static PrintWriter zzz;
    static String password = "0000";

    static void init() {
        dispose("Init : " + "Dispatch");
        //cashRegister = new ActiveXComponent("ArtSoft.DatecsFP3530T");
        //cashDispatch = cashRegister.getObject();

        initWriter("D:/test/test_file.txt");

        //Dispatch.call(cashDispatch, "ShowError", true);
        zzz.println("Dispatch.call(cashDispatch, \"ShowError\")" + " " + true);

        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }

    public static void initWriter(String path) {
        try {
            zzz = new PrintWriter(new FileOutputStream(new File(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void closeWriter() {
        zzz.close();
    }

    public static String getError() {
        //Integer lastError =  cashRegister.getProperty("lastError").getInt();
        //String lastErrorText =  cashRegister.getProperty("lastErrorText").getString();
        Integer lastError = -1;
        String lastErrorText = "Some error";
        zzz.println("cashDispatch.call(cashDispatch, \"lastError)\")" + " " + lastError);
        zzz.println("cashDispatch.call(cashDispatch, \"lastErrorText)\")" + " " + lastErrorText);
        closeWriter();
        return lastErrorText;
    }

    public static void openPort(int comPort, int baudRate) {
        //Dispatch.call(cashDispatch, "OpenPort", comPort, baudRate);
        zzz.println("Dispatch.call(cashDispatch, \"OpenPort\")" + " " + comPort + " " + baudRate);
    }

    public static void closePort() {
        //Dispatch.call(cashDispatch, "ClosePort");
        zzz.println("Dispatch.call(cashDispatch, \"ClosePort\")");
    }

    public static void setArticle(int iCode, ReceiptItem item) {
        //Dispatch.call(cashDispatch, "SetArticle", iCode, item.taxNumber, item.group, item.price, password, item.name);
        zzz.println("Dispatch.call(cashDispatch, \"SetArticle\")" + " " + iCode + " " + item.taxNumber + " " + item.group + " " + item.price + " " + password + " " + item.name);
    }

    public static void openReceipt(int operatorNumber, int placeNumber, boolean sale) {
        //Dispatch.call(cashDispatch, sale? "OpenFiscalReceipt" : "OpenReturnReceipt", iOperator, password, iPlaceNumber);
        zzz.println("Dispatch.call(cashDispatch, " + (sale ? "OpenFiscalReceipt " : "OpenReturnReceipt ") + operatorNumber + " " + password + " " + placeNumber);
    }

    public static void closeReceipt(int operatorNumber, int placeNumber, boolean sale) {
        //Dispatch.call(cashDispatch, "CloseFiscalReceipt");
        zzz.println("Dispatch.call(cashDispatch, " + (sale ? "CloseFiscalReceipt " : "CloseReturnReceipt ") + operatorNumber + " " + password + " " + placeNumber);

    }

    public static void registerItem(ReceiptItem item) {
        //Dispatch.call(cashDispatch, "RegisterItem", item.artNumber, item.quantity, 0, item.articleDiscSum);
        String itemMessage = "Dispatch.call(cashDispatch, \"RegisterItem\")" + " " + item.artNumber + " " + item.quantity + " " + 0 + " " + item.articleDiscSum;
        zzz.println(itemMessage);
    }

    public static void printFiscalText(String msg) {
        //Dispatch.call(cashDispatch, "PrintFiscalText", msg);
        zzz.println("Dispatch.call(cashDispatch, \"PrintFiscalText\")" + " " + msg);
    }

    public static void totalCash(Double sum) {
        //Dispatch.call(cashDispatch, "Total", "Наличными: ", 1, sum);
        zzz.println("Dispatch.call(cashDispatch, \"Total\")" + " " + "Наличными: " + " " + 1 + " " + sum);
    }

    public static void totalCard(Double sum) {
        //Dispatch.call(cashDispatch, "Total", "Карточкой: ", 4, sum);
        zzz.println("Dispatch.call(cashDispatch, \"Total\")" + " " + "Карточкой: " + " " + 4 + " " + sum);
    }

    public static void xReport() {
        //Dispatch.call(cashDispatch, "XReport", password);
        zzz.println("Dispatch.call(cashDispatch, \"XReport\")" + " " + password);
    }

    public static void zReport() {
        //Dispatch.call(cashDispatch, "ZReport", password);
        zzz.println("Dispatch.call(cashDispatch, \"ZReport\")" + " " + password);
    }

    public static Double getCurrentSums(int type) {
        //Dispatch.call(cashDispatch, "getCurrentSums", 2);
        zzz.println("Dispatch.call(cashDispatch, \"getCurrentSums\")" + " " + 2);
        //return cashRegister.getProperty("s1").getDouble() + cashRegister.getProperty("s2").getDouble() +
        //        cashRegister.getProperty("s3").getDouble() + cashRegister.getProperty("s4").getDouble() +
        //        cashRegister.getProperty("s5").getDouble();
        return 500.0;
    }

    public static void setOperatorName(UpdateDataOperator operator) {
        //Dispatch.call(cashDispatch, "SetOperatorName", operator.operatorNumber, password, operator.operatorName);
        zzz.println("Dispatch.call(cashDispatch, \"SetOperatorName\")" + " " + operator.operatorNumber + " " + password + " " + operator.operatorName);
    }

    public static void setMulDecCurRF(String code, Double[] rates) {
        //Dispatch.call(cashDispatch, "SetMulDecCurRF", "0000", 2, code, rates[0], rates[1], rates[2], rates[3]);
        zzz.println("Dispatch.call(cashDispatch, \"SetMulDecCurRF\")" + " " + "0000" + " " + 2 + " " + code + " " + rates[0] + " " + rates[1] + " " + rates[2] + " " + rates[3]);
    }

    public static void advancePaper(int lines) {
        //Dispatch.call(cashDispatch, "AdvancePaper", lines);
        zzz.println("Dispatch.call(cashDispatch, \"AdvancePaper\")" + " " + lines);
    }

    public static void cutReceipt() {
        //Dispatch.call(cashDispatch, "CutReceipt");
        zzz.println("Dispatch.call(cashDispatch, \"CutReceipt\")");
    }

    public static void printTaxReport() {
        Date date = new Date(System.currentTimeMillis());
        String dateFrom = fillZeros(date.getDate()) + fillZeros(date.getMonth()) + fillZeros(date.getYear());
        String dateTo = fillZeros(date.getDate()) + fillZeros(date.getMonth()) + fillZeros(date.getYear());
        //Dispatch.call(cashDispatch, "PrintTaxReport", password, dateFrom, dateTo);
        zzz.println("Dispatch.call(cashDispatch, \"PrintTaxReport\")" + " " + password + " " + dateFrom + " " + dateTo);
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

    private static String fillZeros(int i) {
        String s = String.valueOf(i);
        while (s.length() < 2)
            s = "0" + s;
        return s;
    }
}

