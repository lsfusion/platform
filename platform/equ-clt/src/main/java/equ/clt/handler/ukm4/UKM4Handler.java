package equ.clt.handler.ukm4;

import equ.api.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.xBaseJException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
                    MODDATE.put(new GregorianCalendar(transactionInfo.date.getYear() + 1900, transactionInfo.date.getMonth() + 1, transactionInfo.date.getDay()));/*transactionInfo.date*/
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
        Map<String, String> cashRegisterDirectories = new HashMap<String, String>();
        for (CashRegisterInfo cashRegister : cashRegisterInfoList) {
            if ((cashRegister.directory != null) && (!cashRegisterDirectories.containsValue(cashRegister.directory)))
                cashRegisterDirectories.put(cashRegister.cashRegisterNumber, cashRegister.directory);
            if ((cashRegister.port != null) && (!cashRegisterDirectories.containsValue(cashRegister.port)))
                cashRegisterDirectories.put(cashRegister.cashRegisterNumber, cashRegister.port);
        }
        List<SalesInfo> salesInfoList = new ArrayList<SalesInfo>();
        List<String> readFiles = new ArrayList<String>();
        for (Map.Entry<String, String> entry : cashRegisterDirectories.entrySet()) {
            DBF importSailFile = null;
            DBF importDiscFile = null;
            DBF importCardFile = null;
            Map<String, Double> discountMap = new HashMap<String, Double>();
            Map<String, String> discountCardMap = new HashMap<String, String>();
            try {
                if (entry.getValue() != null) {

                    String fileDiscPath = entry.getValue().trim() + "/CASHDISC.DBF";
                    if (new File(fileDiscPath).exists()) {
                        importDiscFile = new DBF(fileDiscPath);
                        readFiles.add(fileDiscPath);
                        int recordDiscCount = importDiscFile.getRecordCount();
                        for (int i = 0; i < recordDiscCount; i++) {
                            importDiscFile.read();

                            String cashRegisterNumber = new String(importDiscFile.getField("CASHNUMBER").getBytes(), "Cp1251").trim();
                            String zNumber = new String(importDiscFile.getField("ZNUMBER").getBytes(), "Cp1251").trim();
                            Integer receiptNumber = new Integer(new String(importDiscFile.getField("CHECKNUMBE").getBytes(), "Cp1251").trim());
                            Integer numberReceiptDetail = new Integer(new String(importDiscFile.getField("ID").getBytes(), "Cp1251").trim());
                            Integer type = new Integer(new String(importDiscFile.getField("DISCOUNTIN").getBytes(), "Cp1251").trim());
                            Double discountSum = new Double(new String(importDiscFile.getField("DISCOUNTRU").getBytes(), "Cp1251").trim());

                            String sid = cashRegisterNumber + "_" + zNumber + "_" + receiptNumber + "_" + numberReceiptDetail;
                            if (type.equals(4)) {
                                Double tempSum = discountMap.get(sid);
                                discountMap.put(sid, discountSum + (tempSum == null ? 0 : tempSum));
                            }
                        }
                        importDiscFile.close();
                    }

                    String fileCardPath = entry.getValue().trim() + "/CASHDCRD.DBF";
                    if (new File(fileCardPath).exists()) {
                        importCardFile = new DBF(fileCardPath);
                        readFiles.add(fileCardPath);
                        int recordCardCount = importCardFile.getRecordCount();
                        for (int i = 0; i < recordCardCount; i++) {
                            importCardFile.read();

                            String cashRegisterNumber = new String(importCardFile.getField("CASHNUMBER").getBytes(), "Cp1251").trim();
                            String zNumber = new String(importCardFile.getField("ZNUMBER").getBytes(), "Cp1251").trim();
                            Integer receiptNumber = new Integer(new String(importCardFile.getField("CHECKNUMBE").getBytes(), "Cp1251").trim());
                            String cardNumber = new String(importCardFile.getField("CARDNUMBER").getBytes(), "Cp1251").trim();
                            //Integer cardNumber = new Integer(cardNumberString.substring(cardNumberString.length()-4, cardNumberString.length()));

                            String sid = cashRegisterNumber + "_" + zNumber + "_" + receiptNumber;
                            discountCardMap.put(sid, cardNumber);
                        }
                        importCardFile.close();
                    }

                    String fileSailPath = entry.getValue().trim() + "/CASHSAIL.DBF";
                    if (new File(fileSailPath).exists()) {
                        importSailFile = new DBF(fileSailPath);
                        readFiles.add(fileSailPath);
                        int recordSailCount = importSailFile.getRecordCount();
                        Map<Integer, Double[]> receiptNumberSumReceipt = new HashMap<Integer, Double[]>();

                        for (int i = 0; i < /*recordSailCount*/87; i++) {
                            importSailFile.read();

                            Integer operation = new Integer(new String(importSailFile.getField("OPERATION").getBytes(), "Cp1251").trim());
                            //0 - возврат cash, 1 - продажа cash, 2,4 - возврат card, 3,5 - продажа card

                            String cashRegisterNumber = new String(importSailFile.getField("CASHNUMBER").getBytes(), "Cp1251").trim();
                            String zNumber = new String(importSailFile.getField("ZNUMBER").getBytes(), "Cp1251").trim();
                            Integer receiptNumber = new Integer(new String(importSailFile.getField("CHECKNUMBE").getBytes(), "Cp1251").trim());
                            Integer numberReceiptDetail = new Integer(new String(importSailFile.getField("ID").getBytes(), "Cp1251").trim());
                            java.sql.Date date = new java.sql.Date(new SimpleDateFormat("yyyymmdd").parse(new String(importSailFile.getField("DATE").getBytes(), "Cp1251").trim()).getTime());
                            String timeString = new String(importSailFile.getField("TIME").getBytes(), "Cp1251").trim();
                            timeString = timeString.length() == 3 ? ("0" + timeString) : timeString;
                            java.sql.Time time = new java.sql.Time(DateUtils.parseDate(timeString, new String[]{"hhmm"}).getTime());
                            String barcodeReceiptDetail = new String(importSailFile.getField("CARDARTICU").getBytes(), "Cp1251").trim();
                            Double quantityReceiptDetail = new Double(new String(importSailFile.getField("QUANTITY").getBytes(), "Cp1251").trim());
                            Double priceReceiptDetail = new Double(new String(importSailFile.getField("PRICERUB").getBytes(), "Cp1251").trim());
                            Double sumReceiptDetail = new Double(new String(importSailFile.getField("TOTALRUB").getBytes(), "Cp1251").trim());
                            Double discountSumReceiptDetail = discountMap.get(cashRegisterNumber + "_" + zNumber + "_" + receiptNumber + "_" + numberReceiptDetail);
                            String discountCardNumber = discountCardMap.get(cashRegisterNumber + "_" + zNumber + "_" + receiptNumber);
                            
                            Double[] tempSumReceipt = receiptNumberSumReceipt.get(receiptNumber);
                            receiptNumberSumReceipt.put(receiptNumber, new Double[]{(tempSumReceipt != null ? tempSumReceipt[0] : 0) + (operation <= 1 ? sumReceiptDetail : 0),
                                    (tempSumReceipt != null ? tempSumReceipt[1] : 0) + (operation > 1 ? sumReceiptDetail : 0)});

                            salesInfoList.add(new SalesInfo(cashRegisterNumber, zNumber, receiptNumber, date, time, 0.0, 0.0, 0.0,
                                    barcodeReceiptDetail, quantityReceiptDetail * (operation % 2 == 1 ? 1 : -1), priceReceiptDetail, sumReceiptDetail * (operation % 2 == 1 ? 1 : -1),
                                    discountSumReceiptDetail, null, discountCardNumber, numberReceiptDetail, null));
                        }
                        for (SalesInfo salesInfo : salesInfoList) {
                            salesInfo.sumCash = receiptNumberSumReceipt.get(salesInfo.receiptNumber)[0];
                            salesInfo.sumCard = receiptNumberSumReceipt.get(salesInfo.receiptNumber)[1];
                            salesInfo.sumReceipt = salesInfo.sumCash + salesInfo.sumCard;
                        }
                    }
                }
            } catch (xBaseJException e) {
                throw new RuntimeException(e.toString(), e.getCause());
            } finally {
                if (importSailFile != null)
                    importSailFile.close();
                if (importCardFile != null)
                    importCardFile.close();
                if (importDiscFile != null)
                    importDiscFile.close();
            }
        }
        return new UKM4SalesBatch(salesInfoList, readFiles);
    }

    @Override
    public void finishReadingSalesInfo(UKM4SalesBatch salesBatch) {
        for (String readFile : salesBatch.readFiles) {
            File f = new File(readFile);
            if (!f.delete())
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
        }
    }
}
