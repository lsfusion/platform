package equ.clt.handler.kristal;

import equ.api.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.xBaseJException;

import java.io.*;
import java.sql.Time;
import java.text.ParseException;
import java.util.*;

public class KristalHandler extends CashRegisterHandler<KristalSalesBatch> {

    public KristalHandler() {
    }

    @Override
    public void sendTransaction(TransactionCashRegisterInfo transactionInfo, List<CashRegisterInfo> machineryInfoList) throws IOException {

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
            folder = new File(directory.trim() + "/Import");
            if (!folder.exists() && !folder.mkdir())
                throw new RuntimeException("The folder " + folder.getAbsolutePath() + " can not be created");

            Util.setxBaseJProperty("ignoreMissingMDX", "true");

            String path = directory + "/Import/groups.txt";

            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(path), "windows-1251"));

            Set<Integer> numberGroupItems = new HashSet<Integer>();
            for (ItemInfo item : transactionInfo.itemsList) {
                if (!numberGroupItems.contains(item.numberGroupItem)) {
                    String record = "+|" + item.nameGroupItem + "|" + item.numberGroupItem + "|0|0|0|0";
                    writer.println(record);
                    numberGroupItems.add(item.numberGroupItem);
                }

            }
            writer.close();

            path = directory + "/Import/message.txt";
            writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(path), "windows-1251"));

            for (ItemInfo item : transactionInfo.itemsList) {
                if (item.composition != null && !item.composition.equals("")) {
                    String record = "+|" + item.idBarcode + "|" + item.composition + "|||";
                    writer.println(record);
                }
            }
            writer.close();

            path = directory + "/Import/plu.txt";
            writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(path), "windows-1251"));

            for (ItemInfo item : transactionInfo.itemsList) {
                String record = "+|" + item.idBarcode + "|" + item.idBarcode + "|" + item.name + "|" +
                        (item.isWeightItem ? "кг.|" : "ШТ|") + (item.isWeightItem ? "1|" : "0|") + "1|"/*section*/ +
                        item.price.intValue() + "|" + "0|"/*fixprice*/ + (item.isWeightItem ? "0.001|" : "1|") +
                        item.numberGroupItem + "|0|0|0|0";
                writer.println(record);
            }
            writer.close();

            path = directory + "/Import/scales.txt";
            writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(path), "windows-1251"));

            for (ItemInfo item : transactionInfo.itemsList) {
                String record = "+|" + item.idBarcode + "|" + item.idBarcode + "|" + "22|" + item.name + "||" +
                        "1|0|1|"/*effectiveLife & GoodLinkToScales*/ +
                        (item.composition != null ? item.idBarcode : "0")/*ingredientNumber*/ + "|" +
                        item.price.intValue();
                writer.println(record);
            }
            writer.close();
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
        List<String> filePathList = new ArrayList<String>();
        for (Map.Entry<String, String> entry : cashRegisterDirectories.entrySet()) {

            try {
                if (entry.getValue() != null) {
                    String billFilePath = entry.getValue().trim() + "/Export/data/CH_HEAD.dbf";
                    DBF billFile = new DBF(billFilePath);

                    Map<Integer, BillInfo> billInfoMap = new HashMap<Integer, BillInfo>();

                    for (int i = 0; i < billFile.getRecordCount(); i++) {

                        billFile.read();

                        Integer zReportNumber = new Integer(new String(billFile.getField("CREG").getBytes(), "Cp1251").trim());
                        Integer billNumber = new Integer(new String(billFile.getField("ID").getBytes(), "Cp1251").trim());
                        java.sql.Date date = new java.sql.Date(DateUtils.parseDate(new String(billFile.getField("DATE").getBytes(), "Cp1251").trim(), new String[]{"dd.MM.yyyy hh:mm", "dd.MM.yyyy hh:mm:"}).getTime());
                        Time time = new Time(date.getTime());
                        Double cost1 = new Double(new String(billFile.getField("COST1").getBytes(), "Cp1251").trim()); //cash
                        Double cost3 = new Double(new String(billFile.getField("COST3").getBytes(), "Cp1251").trim()); //card
                        Double discountSum = new Double(new String(billFile.getField("SUMDISC").getBytes(), "Cp1251"));
                        String cashRegisterNumber = new String(billFile.getField("CASHIER").getBytes(), "Cp1251");
                        billInfoMap.put(billNumber, new BillInfo(zReportNumber, date, time, cost1 + cost3, cost3, cost1, discountSum, cashRegisterNumber));
                    }
                    billFile.close();

                    String billDetailFilePath = entry.getValue().trim() + "/Export/data/CH_POS.dbf";
                    DBF billDetailFile = new DBF(billDetailFilePath);
                    for (int i = 0; i < billDetailFile.getRecordCount()/*111*/; i++) {

                        billDetailFile.read();
                        Integer billNumber = new Integer(new String(billDetailFile.getField("IDHEAD").getBytes(), "Cp1251").trim());
                        BillInfo billInfo = billInfoMap.get(billNumber);
                        if (billInfo != null) {
                            String cashRegisterNumber = new String(billDetailFile.getField("CASHIER").getBytes(), "Cp1251").trim();
                            String zReportNumber = new String(billDetailFile.getField("CREG").getBytes(), "Cp1251").trim();
                            String barcode = new String(billDetailFile.getField("BARCODE").getBytes(), "Cp1251").trim();
                            Double quantity = new Double(new String(billDetailFile.getField("COUNT").getBytes(), "Cp1251").trim());
                            Double price = new Double(new String(billDetailFile.getField("PRICE").getBytes(), "Cp1251").trim());
                            Double sumBillDetail = new Double(new String(billDetailFile.getField("SUM").getBytes(), "Cp1251").trim());
                            salesInfoList.add(new SalesInfo(cashRegisterNumber, zReportNumber, billNumber, billInfo.date,
                                    billInfo.time, billInfo.sumBill, billInfo.sumCard, billInfo.sumCash, barcode, quantity, price, sumBillDetail, null, billInfo.discountSum, null, billInfo.numberBillDetail++, null));
                        }
                    }
                    billDetailFile.close();
                    filePathList.add(billFilePath);
                    filePathList.add(billDetailFilePath);
                }
            } catch (xBaseJException e) {
                throw new RuntimeException(e.toString(), e.getCause());
            }
        }


        return new KristalSalesBatch(salesInfoList, filePathList);
    }

    @Override
    public void finishReadingSalesInfo(KristalSalesBatch salesBatch) {
        for (String readFile : salesBatch.readFiles) {
            File f = new File(readFile);
            if (!f.delete())
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
        }
    }

    private class BillInfo {
        Integer zReportNumber;
        java.sql.Date date;
        Time time;
        Double sumBill;
        Double sumCard;
        Double sumCash;
        Double discountSum;
        String cashRegisterNumber;
        Integer numberBillDetail;

        BillInfo(Integer zReportNumber, java.sql.Date date, Time time, Double sumBill, Double sumCard, Double sumCash, Double discountSum, String cashRegisterNumber) {
            this.zReportNumber = zReportNumber;
            this.date = date;
            this.time = time;
            this.sumBill = sumBill;
            this.sumCard = sumCard;
            this.sumCash = sumCash;
            this.discountSum = discountSum;
            this.cashRegisterNumber = cashRegisterNumber;
            this.numberBillDetail = 1;
        }
    }
}
