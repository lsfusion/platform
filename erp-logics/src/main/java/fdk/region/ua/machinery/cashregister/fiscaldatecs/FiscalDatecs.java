package fdk.region.ua.machinery.cashregister.fiscaldatecs;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

import java.util.Date;

public class FiscalDatecs {

    static Dispatch cashDispatch;
    static ActiveXComponent cashRegister;
    static String password = "0000";

    static void init() {
        cashRegister = new ActiveXComponent("ArtSoft.DatecsFP3530T");
        cashDispatch = cashRegister.getObject();

        //Dispatch.call(cashDispatch, "ShowError", true);

        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }

    public static String getError() {
        String lastErrorText;
        if (cashRegister.getProperty("lastError").getInt() == 30)
            lastErrorText = "Недостаточно наличности в кассе";
        else
            lastErrorText = cashRegister.getProperty("lastErrorText").getString();
        closePort();
        return lastErrorText;
    }

    public static void openPort(int comPort, int baudRate) throws RuntimeException {
        Dispatch.call(cashDispatch, "OpenPort", comPort, baudRate);
        checkErrors(true);
    }

    public static void closePort() throws RuntimeException {
        Dispatch.call(cashDispatch, "ClosePort");
        checkErrors(true);
    }

    public static int getArticlesInfo() throws RuntimeException {
        Dispatch.call(cashDispatch, "GetArticlesInfo");
        checkErrors(true);
        int value = Integer.parseInt(cashRegister.getProperty("s3").getString());
        if (value > 14800)
            return -1;
        else return value;
    }

    public static void setArticle(int iCode, ReceiptItem item) throws RuntimeException {
        Dispatch.call(cashDispatch, "SetArticle", iCode, item.taxNumber, item.group, item.price, password, item.name);
        checkErrors(true);
    }

    public static int openReceipt(int operatorNumber, int placeNumber, boolean sale) throws RuntimeException {
        Dispatch.call(cashDispatch, sale ? "OpenFiscalReceipt" : "OpenReturnReceipt", operatorNumber, password, placeNumber);
        return checkErrors(false);
    }

    public static int closeReceipt() throws RuntimeException {
        Dispatch.call(cashDispatch, "CloseFiscalReceipt");
        return checkErrors(false);
    }

    public static int cancelReceipt() throws RuntimeException {
        Dispatch.call(cashDispatch, "CancelReceipt");
        return checkErrors(false);
    }

    public static int getFiscalClosureStatus() {
        Dispatch.call(cashDispatch, "GetFiscalClosureStatus", true);
        checkErrors(true);
        return Integer.parseInt(cashRegister.getProperty("s1").getString());
    }

    public static int registerItem(ReceiptItem item) throws RuntimeException {
        Dispatch.call(cashDispatch, "RegistrItem", item.artNumber, item.quantity, 0, item.articleDiscSum == null ? 0 : item.articleDiscSum);
        return checkErrors(false);
    }

    public static int printFiscalText(String msg) throws RuntimeException {
        Dispatch.call(cashDispatch, "PrintFiscalText", msg);
        return checkErrors(false);
    }

    public static int totalCash(Double sum, boolean sale) throws RuntimeException {
        if (sum != null) {
            Dispatch.call(cashDispatch, "Total", null, 1, sale ? sum : -sum);
        }
        return checkErrors(false);
    }

    public static int totalCard(Double sum, boolean sale) throws RuntimeException {
        if (sum != null) {
            Dispatch.call(cashDispatch, "Total", null, 4, sale ? sum : -sum);
        }
        return cashRegister.getProperty("lastError").getInt();
    }

    public static void xReport() throws RuntimeException {
        Dispatch.call(cashDispatch, "XReport", password);
        checkErrors(true);
    }

    public static void zReport() throws RuntimeException {
        Dispatch.call(cashDispatch, "ZReport", password);
        checkErrors(true);
    }

    public static Double getCurrentSums(int type) throws RuntimeException {
        Dispatch.call(cashDispatch, "getCurrentSums", type);
        checkErrors(true);
        return (Double.valueOf(cashRegister.getProperty("s1").getString()) + Double.valueOf(cashRegister.getProperty("s2").getString()) +
                Double.valueOf(cashRegister.getProperty("s3").getString()) + Double.valueOf(cashRegister.getProperty("s4").getString()) +
                Double.valueOf(cashRegister.getProperty("s5").getString())) / 100;
    }

    public static void setOperatorName(UpdateDataOperator operator) throws RuntimeException {
        Dispatch.call(cashDispatch, "SetOperatorName", operator.operatorNumber, password, operator.operatorName);
        checkErrors(true);
    }

    public static void setMulDecCurRF(String code, Double[] rates) throws RuntimeException {
        Dispatch.call(cashDispatch, "GetCurrentTaxRates");
        Double s1 = Double.valueOf(cashRegister.getProperty("s1").getString());
        Double s2 = Double.valueOf(cashRegister.getProperty("s2").getString());
        Double s3 = Double.valueOf(cashRegister.getProperty("s3").getString());
        Double s4 = Double.valueOf(cashRegister.getProperty("s4").getString());
        if (!(s1.equals(rates[0])) || !(s2.equals(rates[1])) || !(s3.equals(rates[2])) || !(s4.equals(rates[3])))
            Dispatch.call(cashDispatch, "SetMulDecCurRF", "0000", 2, code, rates[0], rates[1], rates[2], rates[3]);
        checkErrors(true);
    }

    public static void advancePaper(int lines) throws RuntimeException {
        Dispatch.call(cashDispatch, "AdvancePaper", lines);
        checkErrors(true);
    }

    public static void cutReceipt() throws RuntimeException {
        Dispatch.call(cashDispatch, "CutReceipt");
        checkErrors(true);
    }

    public static boolean inOut(Double sum) throws RuntimeException {
        if (sum != null && sum < 0) {
            Dispatch.call(cashDispatch, "InOut", -1000000000);
            String s1 = cashRegister.getProperty("s1").getString();
            Double inCash = Double.valueOf(s1.substring(1, s1.length())) / 100;
            if ((-sum) > inCash) {
                closePort();
                return false;
            }
        }
        Dispatch.call(cashDispatch, "InOut", sum);
        checkErrors(true);
        return true;
    }

    public static void printTaxReport() throws RuntimeException {
        Date date = new Date(System.currentTimeMillis());
        String dateFrom = fillZeros(date.getDate()) + fillZeros(date.getMonth()) + fillZeros(date.getYear());
        String dateTo = fillZeros(date.getDate()) + fillZeros(date.getMonth()) + fillZeros(date.getYear());
        Dispatch.call(cashDispatch, "PrintTaxReport", password, dateFrom, dateTo);
        checkErrors(true);
    }

    public static void delArticle(int iCode) throws RuntimeException {
        Dispatch.call(cashDispatch, "DelArticle", password, iCode);
        checkErrors(true);
    }

    public static void displayText(ReceiptItem item) throws RuntimeException {

        String firstLine = " " + toStr(item.quantity) + "x" + toStr(item.price);
        firstLine = item.name.substring(0, 20 - Math.min(20, firstLine.length())) + firstLine;
        String secondLine = toStr(item.sumPos);
        while (secondLine.length() < 13)
            secondLine = " " + secondLine;
        secondLine = "ВСЬОГО:" + secondLine;
        Dispatch.call(cashDispatch, "DisplayTextUL", firstLine);
        Dispatch.call(cashDispatch, "DisplayTextLL", secondLine);
        checkErrors(true);
    }

    private static String toStr(Double value) {
        if (value == null)
            return "0";
        else {
            boolean isInt = (value - value.intValue()) == 0;
            return isInt ? String.valueOf(value.intValue()) : String.valueOf(value);
        }
    }

    public static int checkErrors(Boolean throwException) throws RuntimeException {
        int lastError = cashRegister.getProperty("lastError").getInt();
        if (lastError != 0) {
            if (throwException)
                throw new RuntimeException("Datecs Exception: " + lastError);
        }
        return lastError;
    }

    private static String fillZeros(int i) {
        String s = String.valueOf(i);
        while (s.length() < 2)
            s = "0" + s;
        return s;
    }
}

