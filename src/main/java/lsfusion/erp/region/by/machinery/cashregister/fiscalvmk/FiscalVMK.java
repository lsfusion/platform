package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

public class FiscalVMK {

    public interface vmkDLL extends Library {

        vmkDLL vmk = (vmkDLL) Native.loadLibrary("vmk", vmkDLL.class);

        Integer vmk_lasterror();

        void vmk_errorstring(Integer error, byte[] buffer, Integer length);

        Boolean vmk_open(String comport, Integer baudrate);

        void vmk_close();

        Boolean vmk_opensmn();

        Boolean vmk_opencheck(Integer type);

        Boolean vmk_cancel();

        Boolean vmk_sale(String coddigit, byte[] codname, Integer codcena, Integer ot, Double quantity,
                         Integer sum);

        Boolean vmk_discount(byte[] name, Integer value, int flag);

        Boolean vmk_subtotal();

        Boolean vmk_prnch(String message);

        Boolean vmk_oplat(Integer type, Integer sum, Integer flagByte);

        Boolean vmk_xotch();

        Boolean vmk_zotch();

        Boolean vmk_feed(int type, int cnt_string, int cnt_dot_line);

        Boolean vmk_vnes(long sum);

        Boolean vmk_vyd(long sum);

        Boolean vmk_opendrawer(int cnt_msek);

        Boolean vmk_indik(byte[] firstLine, byte[] secondLine);

        Boolean vmk_ksastat(ByReference rej, ByReference stat);
    }

    static void init() {

        System.loadLibrary("msvcr100");
        System.loadLibrary("msvcp100");
        System.loadLibrary("QtCore4");
        System.loadLibrary("vmk");

        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }

    public static String getError(boolean closePort) {
        Integer lastError = vmkDLL.vmk.vmk_lasterror();
        int length = 255;
        byte[] lastErrorText = new byte[length];
        vmkDLL.vmk.vmk_errorstring(lastError, lastErrorText, length);
        if (closePort)
            closePort();
        return Native.toString(lastErrorText, "cp1251");
    }

    public static void openPort(int comPort, int baudRate) throws RuntimeException {
        if (!vmkDLL.vmk.vmk_open("COM" + comPort, baudRate))
            checkErrors(true);
    }

    public static void closePort() throws RuntimeException {
        vmkDLL.vmk.vmk_close();
    }

    public static boolean openReceipt(int type) throws RuntimeException {    //0 - продажа, 1 - возврат
        return vmkDLL.vmk.vmk_opencheck(type);
    }

    public static boolean cancelReceipt() throws RuntimeException {
        return vmkDLL.vmk.vmk_cancel();
    }

    public static boolean getFiscalClosureStatus() {
        IntByReference rej = new IntByReference();
        IntByReference stat = new IntByReference();
        if (!vmkDLL.vmk.vmk_ksastat(rej, stat))
            return false;
        if (BigInteger.valueOf(stat.getValue()).testBit(14))
            if (!cancelReceipt())
                return false;
        return true;
    }

    public static boolean printFiscalText(String msg) throws RuntimeException {
        return vmkDLL.vmk.vmk_prnch(msg);
    }

    public static boolean totalCash(Double sum) throws RuntimeException {
        if (sum == null)
            return true;
        return vmkDLL.vmk.vmk_oplat(0, Math.abs(sum.intValue()), 0/*"00000000"*/);
    }

    public static boolean totalCard(Double sum) throws RuntimeException {
        if (sum == null)
            return true;
        return vmkDLL.vmk.vmk_oplat(1, Math.abs(sum.intValue()), 0/*"00000000"*/);
    }

    public static void xReport() throws RuntimeException {
        if (!vmkDLL.vmk.vmk_xotch())
            checkErrors(true);
    }

    public static void zReport() throws RuntimeException {
        if (!vmkDLL.vmk.vmk_zotch())
            checkErrors(true);
    }

    public static void advancePaper(int lines) throws RuntimeException {
        if (!vmkDLL.vmk.vmk_feed(1, lines, 1))
            checkErrors(true);
    }

    public static boolean inOut(Long sum) throws RuntimeException {

        if (sum > 0) {
            if (!vmkDLL.vmk.vmk_vnes(sum))
                checkErrors(true);
        } else {
            if (!vmkDLL.vmk.vmk_vyd(-sum))
                return false;
        }
        return true;
    }

    public static boolean openDrawer() throws RuntimeException {
        return vmkDLL.vmk.vmk_opendrawer(0);
    }

    public static void displayText(ReceiptItem item) throws RuntimeException {
        try {
            String firstLine = " " + toStr(item.quantity) + "x" + toStr((double) item.price);
            firstLine = item.name.substring(0, 16 - Math.min(16, firstLine.length())) + firstLine;
            String secondLine = toStr((double) item.sumPos);
            while (secondLine.length() < 11)
                secondLine = " " + secondLine;
            secondLine = "ИТОГ:" + secondLine;
            if (!vmkDLL.vmk.vmk_indik((firstLine + "\0").getBytes("cp1251"), (new String(secondLine + "\0")).getBytes("cp1251")))
                checkErrors(true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static boolean registerItem(ReceiptItem item) throws RuntimeException {
        try {
            return vmkDLL.vmk.vmk_sale(item.barCode, (item.name+"\0").getBytes("cp1251"), Math.abs(item.price.intValue()), 1 /*отдел*/, item.quantity, 0);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    public static boolean discountItem(ReceiptItem item) throws RuntimeException {
        if (item.articleDiscSum == null)
            return true;
        boolean discount = item.articleDiscSum < 0;
        try {
            return vmkDLL.vmk.vmk_discount(((discount ? "Скидка" : "Наценка") +"\0").getBytes("cp1251"), Math.abs(item.articleDiscSum.intValue()), discount ? 3 : 1);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    public static boolean subtotal() throws RuntimeException {
        if (!vmkDLL.vmk.vmk_subtotal())
            return false;
        return true;
    }

    public static void opensmIfClose() throws RuntimeException {
        IntByReference rej = new IntByReference();
        IntByReference stat = new IntByReference();
        if (!vmkDLL.vmk.vmk_ksastat(rej, stat))
            checkErrors(true);
        if (!BigInteger.valueOf(stat.getValue()).testBit(15))//15 - открыта ли смена
            if (!vmkDLL.vmk.vmk_opensmn())
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
        Integer lastError = vmkDLL.vmk.vmk_lasterror();
        if (lastError != 0) {
            if (throwException)
                throw new RuntimeException("Datecs Exception: " + lastError);
        }
        return lastError;
    }
}

