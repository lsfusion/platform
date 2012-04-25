import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.fields.*;
import org.xBaseJ.xBaseJException;
import retail.api.remote.*;

import java.io.*;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxishopHandler implements CashRegisterHandler<MaxishopSalesBatch> {

    public MaxishopHandler() {
    }

    @Override
    public void sendTransaction(TransactionCashRegisterInfo transactionInfo, List<CashRegisterInfo> machineryInfoList) throws IOException, FileNotFoundException {

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
                folder = new File(directory.trim() + "/SEND");
                folder.mkdir();

                Util.setxBaseJProperty("ignoreMissingMDX", "true");

                String path = directory + "/SEND/" + transactionInfo.dateTimeCode;
                DBF file = new DBF(path + ".DBF", DBF.DBASEIV, true, "CP866");


                file.addField(new Field[]{POSNO, CMD, ERRNO, PLUCODE, ECRID, NAME, PRICE1, PRICE2, PRICE3, PRICE4,
                        PRICE5, PRICE6, SELPRICE, MANPRICE, TAXNO, DISCNO, PLUSUPP, PLUPACK, QUANTPACK, DEPNO, GROUPNO,
                        SERTNO, SERTDATE, PQTY2, PQTY3, PQTY4, PQTY5, PQTY6, PMINPRICE, PSTATUS, PMATVIEN});

                for (ItemInfo item : transactionInfo.itemsList) {
                    PLUCODE.put(item.barcodeEx);
                    NAME.put(item.name);
                    PRICE1.put(item.price);
                    file.write();
                }
                file.close();

                File fileOut = new File(path + ".OUT");
                fileOut.createNewFile();

            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList) {

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
            try {
                if (entry.getValue() != null) {
                    File directory = new File(entry.getValue().trim() + "/READ/");
                    if (directory.isDirectory())
                        for (String fileName : directory.list(new DBFFilter())) {
                            String filePath = entry.getValue().trim() + "/READ/" + fileName;
                            DBF importFile = new DBF(filePath);
                            readFiles.add(filePath);
                            int recordCount = importFile.getRecordCount();
                            int numberBillDetail = 1;
                            Integer oldBillNumber = -1;
                            for (int i = 0; i < recordCount; i++) {
                                importFile.read();
                                String postType = new String(importFile.getField("JFPOSTYPE").getBytes(), "Cp1251").trim();
                                if ("P".equals(postType)) {
                                    Integer zReportNumber = new Integer(new String(importFile.getField("JFZNO").getBytes(), "Cp1251").trim());
                                    Integer billNumber = new Integer(new String(importFile.getField("JFCHECKNO").getBytes(), "Cp1251").trim());
                                    java.sql.Date date = new java.sql.Date(new SimpleDateFormat("yyyymmdd").parse(new String(importFile.getField("JFDATE").getBytes(), "Cp1251").trim()).getTime());
                                    String timeString = new String(importFile.getField("JFTIME").getBytes(), "Cp1251").trim();
                                    Time time = Time.valueOf(timeString.substring(0, 2) + ":" + timeString.substring(2, 4) + ":" + timeString.substring(4, 6));
                                    Double sumBill = new Double(new String(importFile.getField("JFTOTSUM").getBytes(), "Cp1251").trim());
                                    String barcodeBillDetail = new String(importFile.getField("JFPLUCODE").getBytes(), "Cp1251").trim().replace("E", "");
                                    Double quantityBillDetail = new Double(new String(importFile.getField("JFQUANT").getBytes(), "Cp1251").trim());
                                    Double priceBillDetail = new Double(new String(importFile.getField("JFPRICE").getBytes(), "Cp1251").trim());
                                    Double discountSumBillDetail = new Double(new String(importFile.getField("JFDISCSUM").getBytes(), "Cp1251").trim());
                                    Double sumBillDetail = round10(priceBillDetail * quantityBillDetail - discountSumBillDetail);

                                    if (oldBillNumber.equals(billNumber)) {
                                        numberBillDetail = 1;
                                        oldBillNumber = billNumber;
                                    }
                                    salesInfoList.add(new SalesInfo(entry.getKey(), zReportNumber, billNumber, date, time, sumBill, barcodeBillDetail,
                                            quantityBillDetail, priceBillDetail, sumBillDetail, discountSumBillDetail, numberBillDetail, fileName));
                                    numberBillDetail++;
                                }
                            }
                            importFile.close();
                        }
                }
            } catch (xBaseJException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return new MaxishopSalesBatch(salesInfoList, readFiles);
    }

    @Override
    public void finishReadingSalesInfo(MaxishopSalesBatch salesBatch) {
        for (String readFile : salesBatch.readFiles) {
            File f = new File(readFile);
            f.delete();
            f = new File(readFile.substring(0, readFile.length() - 3) + "OUT");
            f.delete();
        }
    }

    class DBFFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".DBF") && new File(dir + "/" + name.substring(0, name.length() - 3) + "OUT").exists());
        }
    }

    private Double round10(Double value) {
        return Double.valueOf(Math.round(value / 10) * 10);
    }
}
