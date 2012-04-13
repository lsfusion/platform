import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.fields.*;
import org.xBaseJ.xBaseJException;
import retail.api.remote.CashRegisterHandler;
import retail.api.remote.CashRegisterInfo;
import retail.api.remote.ItemInfo;
import retail.api.remote.TransactionCashRegisterInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MaxishopHandler implements CashRegisterHandler {

    public MaxishopHandler() {
    }

    @Override
    public void sendTransaction(TransactionCashRegisterInfo transactionInfo, List<CashRegisterInfo> machineryInfoList) throws UnsupportedEncodingException, FileNotFoundException {

        try {
            NumField POSNO = new NumField("POSNO", 5, 0);
            CharField CMD = new CharField("CMD", 1);
            NumField ERRNO = new NumField("ERRNO", 5, 0);
            CharField PLUCODE = new CharField("PLUCODE", 20);
            CharField ECRID = new CharField("ECRID", 10);
            CharField NAME = new CharField("NAME", 100);
            NumField PRICE1 = new NumField("PRICE1", 20, 0);
            NumField PRICE2 = new NumField("PRICE2", 20, 0);
            NumField PRICE3 = new NumField("PRICE3", 20, 0);
            NumField PRICE4 = new NumField("PRICE4", 20, 0);
            NumField PRICE5 = new NumField("PRICE5", 20, 0);
            NumField PRICE6 = new NumField("PRICE6", 20, 0);
            LogicalField SELPRICE = new LogicalField("SELPRICE");
            LogicalField MANPRICE = new LogicalField("MANPRICE");
            NumField TAXNO = new NumField("TAXNO", 5, 0);
            NumField DISCNO = new NumField("DISCNO", 5, 0);
            CharField PLUSUPP = new CharField("PLUSUPP", 10);
            CharField PLUPACK = new CharField("PLUPACK", 10);
            NumField QUANTPACK = new NumField("QUANTPACK", 5, 0);
            NumField DEPNO = new NumField("DEPNO", 5, 0);
            NumField GROUPNO = new NumField("GROUPNO", 5, 0);
            CharField SERTNO = new CharField("SERTNO", 10);
            DateField SERTDATE = new DateField("SERTDATE");
            NumField PQTY2 = new NumField("PQTY2", 5, 0);
            NumField PQTY3 = new NumField("PQTY3", 5, 0);
            NumField PQTY4 = new NumField("PQTY4", 5, 0);
            NumField PQTY5 = new NumField("PQTY5", 5, 0);
            NumField PQTY6 = new NumField("PQTY6", 5, 0);
            NumField PMINPRICE = new NumField("PMINPRICE", 10, 0);
            CharField PSTATUS = new CharField("PSTATUS", 10);
            CharField PMATVIEN = new CharField("PMATVIEN", 10);

            List<String> directoriesList = new ArrayList<String>();
            for (CashRegisterInfo cashRegisterInfo : machineryInfoList) {
                if ((cashRegisterInfo.port != null) && (!directoriesList.contains(cashRegisterInfo.port.trim())))
                    directoriesList.add(cashRegisterInfo.port.trim());
                if ((cashRegisterInfo.directory != null) && (!directoriesList.contains(cashRegisterInfo.directory.trim())))
                    directoriesList.add(cashRegisterInfo.directory.trim());
            }

            for (String directory : directoriesList) {
                File folder = new File(directory.trim());
                folder.mkdir();
                folder = new File(directory.trim()+"/SEND");
                folder.mkdir();

                Util.setxBaseJProperty("ignoreMissingMDX", "true");

                DBF file = new DBF(directory + "/SEND/" + transactionInfo.dateTimeCode + ".dbf", DBF.DBASEIV, true, "CP866");



                file.addField(new Field[] {POSNO, CMD, ERRNO, PLUCODE, ECRID, NAME, PRICE1, PRICE2, PRICE3, PRICE4,
                PRICE5, PRICE6, SELPRICE, MANPRICE, TAXNO, DISCNO, PLUSUPP, PLUPACK, QUANTPACK, DEPNO, GROUPNO,
                SERTNO, SERTDATE, PQTY2, PQTY3, PQTY4, PQTY5, PQTY6, PMINPRICE, PSTATUS, PMATVIEN});

                for (ItemInfo item : transactionInfo.itemsList) {
                    PLUCODE.put(item.barcodeEx);
                    NAME.put(item.name);
                    PRICE1.put(item.price);
                    file.write();
                }
                file.close();
            }
        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
