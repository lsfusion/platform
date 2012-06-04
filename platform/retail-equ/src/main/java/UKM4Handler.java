import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.xBaseJException;
import retail.api.remote.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

public class UKM4Handler extends CashRegisterHandler<UKM4SalesBatch> {

    public UKM4Handler() {
    }

    @Override
    public void sendTransaction(TransactionCashRegisterInfo transactionInfo, List<CashRegisterInfo> machineryInfoList) throws IOException {

        DBF fileBar = null;
        DBF fileClassif = null;
        DBF filePlucash = null;
        DBF filePlulim = null;

        try {
            NumField BARCODE = new NumField("BARCODE", 10, 0);
            NumField CARDARTICU = new NumField("CARDARTICU", 14, 0);
            CharField CARDSIZE = new CharField("CARDSIZE", 20);
            NumField QUANTITY = new NumField("QUANTITY", 10, 0);

            NumField ARTICUL = new NumField("ARTICUL", 10, 0);
            CharField NAME = new CharField("NAME", 50);
            CharField MESURIMENT = new CharField("MESURIMENT", 2);
            NumField MESPRESISI = new NumField("MESPRESISI", 10, 0);
            CharField ADD1 = new CharField("ADD1", 10);
            CharField ADD2 = new CharField("ADD2", 10);
            CharField ADD3 = new CharField("ADD3", 10);
            CharField ADDNUM1 = new CharField("ADDNUM1", 10);
            CharField ADDNUM2 = new CharField("ADDNUM2", 10);
            CharField ADDNUM3 = new CharField("ADDNUM3", 10);
            CharField SCALE = new CharField("SCALE", 20);
            NumField GROOP1 = new NumField("GROOP1", 10, 0);
            NumField GROOP2 = new NumField("GROOP2", 10, 0);
            NumField GROOP3 = new NumField("GROOP3", 10, 0);
            NumField GROOP4 = new NumField("GROOP4", 10, 0);
            NumField GROOP5 = new NumField("GROOP5", 10, 0);
            NumField PRICERUB = new NumField("PRICERUB", 10, 0);
            CharField PRICECUR = new CharField("PRICECUR", 10);
            NumField CLIENTINDE = new NumField("CLIENTINDE", 10, 0);
            CharField COMMENTARY = new CharField("COMMENTARY", 50);
            NumField DELETED = new NumField("DELETED", 1, 0);
            DateField MODDATE = new DateField("MODDATE");
            CharField MODTIME = new CharField("MODTIME", 20);
            CharField MODPERSONI = new CharField("MODPERSONI", 50);

            NumField PERCENT = new NumField("PERCENT", 5, 0);

            List<String> directoriesList = new ArrayList<String>();
            for (CashRegisterInfo cashRegisterInfo : machineryInfoList) {
                if ((cashRegisterInfo.port != null) && (!directoriesList.contains(cashRegisterInfo.port.trim())))
                    directoriesList.add(cashRegisterInfo.port.trim());
                if ((cashRegisterInfo.directory != null) && (!directoriesList.contains(cashRegisterInfo.directory.trim())))
                    directoriesList.add(cashRegisterInfo.directory.trim());
            }

            for (String directory : directoriesList) {
                File folder = new File(directory.trim());
                if (!folder.exists() && !folder.mkdir())
                    throw new RuntimeException("The folder " + folder.getAbsolutePath() + " can not be created");
                folder = new File(directory.trim() + "/tovar");
                if (!folder.exists() && !folder.mkdir())
                    throw new RuntimeException("The folder " + folder.getAbsolutePath() + " can not be created");

                Util.setxBaseJProperty("ignoreMissingMDX", "true");

                String path = directory + "/tovar/BAR.DBF";
                fileBar = new DBF(path, DBF.DBASEIV, true, "CP866");
                fileBar.addField(new Field[]{BARCODE, CARDARTICU, CARDSIZE, QUANTITY});

                for (ItemInfo item : transactionInfo.itemsList) {
                    BARCODE.put(item.idBarcode);
                    CARDARTICU.put(item.idBarcode); //или что туда надо писать?
                    CARDSIZE.put("NOSIZE");
                    QUANTITY.put(1); //без разницы, что писать в количество?
                    fileBar.write();
                    fileBar.file.setLength(fileBar.file.length() - 1);
                }


                path = directory + "/tovar/CLASSIF.DBF";
                fileClassif = new DBF(path, DBF.DBASEIV, true, "CP866");
                fileClassif.addField(new Field[]{GROOP1, GROOP2, GROOP3, GROOP4, GROOP5, NAME});

                for (ItemInfo item : transactionInfo.itemsList) {
                    NAME.put(item.name);
                    //А группы откуда брать?
                    fileClassif.write();
                    fileClassif.file.setLength(fileClassif.file.length() - 1);
                }

                path = directory + "/tovar/PLUCASH.DBF";
                filePlucash = new DBF(path, DBF.DBASEIV, true, "CP866");
                filePlucash.addField(new Field[]{ARTICUL, NAME, MESURIMENT, MESPRESISI, ADD1, ADD2, ADD3, ADDNUM1,
                        ADDNUM2, ADDNUM3, SCALE, GROOP1, GROOP2, GROOP3, GROOP4, GROOP5, PRICERUB, PRICECUR,
                        CLIENTINDE, COMMENTARY, DELETED, MODDATE, MODTIME, MODPERSONI
                });

                for (ItemInfo item : transactionInfo.itemsList) {
                    ARTICUL.put(item.idBarcode);
                    NAME.put(item.name);
                    MESURIMENT.put(item.isWeightItem ? "кг" : "1");
                    MESPRESISI.put(item.isWeightItem ? 0.001 : 1.000);
                    SCALE.put("NOSIZE");
                    GROOP1.put(0); //классификация
                    GROOP2.put(0);
                    GROOP3.put(0);
                    GROOP4.put(0);
                    GROOP5.put(1);
                    PRICERUB.put(item.price);
                    CLIENTINDE.put(0);
                    DELETED.put(1);
                    MODDATE.put(new GregorianCalendar(transactionInfo.date.getYear() + 1900, transactionInfo.date.getMonth()+1, transactionInfo.date.getDay()));/*transactionInfo.date*/
                    filePlucash.write();
                    filePlucash.file.setLength(filePlucash.file.length() - 1);
                }

                path = directory + "/tovar/PLULIM.DBF";
                filePlulim = new DBF(path, DBF.DBASEIV, true, "CP866");
                filePlulim.addField(new Field[]{CARDARTICU, PERCENT});

                for (ItemInfo item : transactionInfo.itemsList) {
                    CARDARTICU.put(item.idBarcode);
                    PERCENT.put(0); //откуда брать макс. процент скидки?
                    filePlulim.write();
                    filePlulim.file.setLength(filePlulim.file.length() - 1);
                }

            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        } finally {
            if (fileBar != null)
                fileBar.close();
            if (filePlucash != null)
                filePlucash.close();
            if (filePlulim != null)
                filePlulim.close();
            if (fileClassif != null)
                fileClassif.close();
        }
    }

    @Override
    public SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException {
        return null;
    }

    @Override
    public void finishReadingSalesInfo(UKM4SalesBatch salesBatch) {
    }
}
