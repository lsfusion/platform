import org.xBaseJ.Util;
import retail.api.remote.*;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return new KristalSalesBatch(null);
    }

    @Override
    public void finishReadingSalesInfo(KristalSalesBatch salesBatch) {
    }
}
