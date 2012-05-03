import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.indexes.Index;
import org.xBaseJ.xBaseJException;
import retail.api.remote.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class InventoryTechHandler implements TerminalHandler<InventoryTechSalesBatch> {

    public InventoryTechHandler() {
    }

    @Override
    public void sendTransaction(TransactionTerminalInfo transactionInfo, List<TerminalInfo> machineryInfoList) throws IOException {

        try {

            List<String> directoriesList = new ArrayList<String>();
            for (TerminalInfo terminalInfo : machineryInfoList) {
                if ((terminalInfo.port != null) && (!directoriesList.contains(terminalInfo.port.trim())))
                    directoriesList.add(terminalInfo.port.trim());
                if ((terminalInfo.directory != null) && (!directoriesList.contains(terminalInfo.directory.trim())))
                    directoriesList.add(terminalInfo.directory.trim());
            }

            for (String directory : directoriesList) {
                File folder = new File(directory.trim());
                if (!folder.exists() && !folder.mkdir())
                    throw new RuntimeException("The folder " + folder.getAbsolutePath() + " can not be created");

                Util.setxBaseJProperty("ignoreMissingMDX", "true");

                DBF fileBarcode = new DBF(directory + "/BARCODE.DBF", "CP866");
                Index fileBarcodeIndex = fileBarcode.createIndex(directory + "/" + transactionInfo.dateTimeCode + "B.NDX", "BARCODE", true, true);

                for (ItemInfo item : transactionInfo.itemsList) {
                    if (fileBarcode.findExact(item.barcodeEx)) {
                        fileBarcode.getField("ARTICUL").put(item.barcodeEx);
                        fileBarcode.getField("BARCODE").put(item.barcodeEx);
                        fileBarcode.update();
                    } else {
                        fileBarcode.getField("ARTICUL").put(item.barcodeEx);
                        fileBarcode.getField("BARCODE").put(item.barcodeEx);
                        fileBarcode.write();
                        fileBarcode.file.setLength(fileBarcode.file.length()-1);
                    }
                }
                fileBarcode.close();
                if(!fileBarcodeIndex.file.delete())
                    throw new RuntimeException("File" + fileBarcodeIndex.file.getAbsolutePath() + " can not be deleted");

                DBF fileGoods = new DBF(directory + "/GOODS.DBF", "CP866");
                Index fileGoodsIndex = fileGoods.createIndex(directory + "/" + transactionInfo.dateTimeCode + "G.NDX", "ARTICUL", true, true);

                for (ItemInfo item : transactionInfo.itemsList) {
                    if (fileGoods.findExact(item.barcodeEx)) {
                        fileGoods.getField("ARTICUL").put(item.barcodeEx);
                        fileGoods.getField("NAME").put(item.name);
                        fileGoods.getField("PRICE").put(item.price.toString());
                        fileGoods.update();
                    } else {
                        fileGoods.getField("ARTICUL").put(item.barcodeEx);
                        fileGoods.getField("NAME").put(item.name);
                        fileGoods.getField("PRICE").put(item.price.toString());
                        fileGoods.write();
                        fileGoods.file.setLength(fileGoods.file.length()-1);
                    }
                }
                fileGoods.close();
                if(!fileGoodsIndex.file.delete())
                    throw new RuntimeException("File" + fileGoodsIndex.file.getAbsolutePath() + " can not be deleted");
            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        }
    }

    @Override
    public SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException {
        /*Map<String, String> cashRegisterDirectories = new HashMap<String, String>();
        Map<String, Integer> cashRegisterRoundSales = new HashMap<String, Integer>();
        for (CashRegisterInfo cashRegister : cashRegisterInfoList) {
            if ((cashRegister.directory != null) && (!cashRegisterDirectories.containsValue(cashRegister.directory)))
                cashRegisterDirectories.put(cashRegister.cashRegisterNumber, cashRegister.directory);
            if ((cashRegister.port != null) && (!cashRegisterDirectories.containsValue(cashRegister.port)))
                cashRegisterDirectories.put(cashRegister.cashRegisterNumber, cashRegister.port);
            if (cashRegister.roundSales != null)
                cashRegisterRoundSales.put(cashRegister.cashRegisterNumber, cashRegister.roundSales);
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
                                    Double sumBillDetail = roundSales(priceBillDetail * quantityBillDetail - discountSumBillDetail, cashRegisterRoundSales.get(entry.getKey()));

                                    if (!oldBillNumber.equals(billNumber)) {
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
                throw new RuntimeException(e.toString(), e.getCause());
            }
        }*/

        return new InventoryTechSalesBatch(/*salesInfoList, readFiles*/null, null);
    }

    @Override
    public void finishReadingSalesInfo(InventoryTechSalesBatch salesBatch) {
        /*for (String readFile : salesBatch.readFiles) {
            File f = new File(readFile.substring(0, readFile.length() - 3) + "OUT");
            if (!f.delete())
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
            f = new File(readFile);
            if (!f.delete())
                throw new RuntimeException("The file " + f.getAbsolutePath() + " can not be deleted");
        }*/
    }

    /*   class DBFFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".dbf") && new File(dir + "/" + name.substring(0, name.length() - 3) + "OUT").exists());
        }
    }*/

    /* private Double roundSales(Double value, Integer roundSales) {
        Integer round = roundSales != null ? roundSales : 50;
        return (double) Math.round(value / round) * round;
    }*/
}
