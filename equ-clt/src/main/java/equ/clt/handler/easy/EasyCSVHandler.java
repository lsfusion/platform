package equ.clt.handler.easy;

import equ.api.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EasyCSVHandler {

    public EasyCSVHandler() {

    }

    public class EasyCashRegisterCSVHandler extends CashRegisterHandler<SalesBatch> {

        public EasyCashRegisterCSVHandler() {
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
                folder.mkdir();
                File f = new File(directory.trim() + "/" + transactionInfo.dateTimeCode + ".csv");
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(f), "windows-1251"));
                for (ItemInfo item : transactionInfo.itemsList) {
                    String row = item.idBarcode + ";" + item.name + ";" + item.price;
                    writer.println(row);
                }
                writer.close();
            }
        }

        @Override
        public SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void finishReadingSalesInfo(SalesBatch salesBatch) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    public class EasyPriceCheckerCSVHandler extends PriceCheckerHandler {

        public EasyPriceCheckerCSVHandler() {
        }

        @Override
        public void sendTransaction(TransactionPriceCheckerInfo transactionInfo, List<PriceCheckerInfo> machineryInfoList) throws IOException {

            List<String> directoriesList = new ArrayList<String>();
            for (PriceCheckerInfo priceCheckerInfo : machineryInfoList) {
                if ((priceCheckerInfo.port != null) && (!directoriesList.contains(priceCheckerInfo.port.trim())))
                    directoriesList.add(priceCheckerInfo.port.trim());
            }

            for (String directory : directoriesList) {
                File folder = new File(directory.trim());
                folder.mkdir();
                File f = new File(directory.trim() + "/" + transactionInfo.dateTimeCode + ".csv");
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(f), "windows-1251"));
                for (ItemInfo item : transactionInfo.itemsList) {
                    String row = item.idBarcode + ";" + item.name + ";" + item.price;
                    writer.println(row);
                }
                writer.close();
            }
        }
    }

    public class EasyScalesCSVHandler extends ScalesHandler{

        public EasyScalesCSVHandler() {
        }

        @Override
        public void sendTransaction(TransactionScalesInfo transactionInfo, List<ScalesInfo> machineryInfoList) throws IOException {

            List<String> directoriesList = new ArrayList<String>();
            for (ScalesInfo scalesInfo : machineryInfoList) {
                if ((scalesInfo.port != null) && (!directoriesList.contains(scalesInfo.port.trim())))
                    directoriesList.add(scalesInfo.port.trim());
                if ((scalesInfo.directory != null) && (!directoriesList.contains(scalesInfo.directory.trim())))
                    directoriesList.add(scalesInfo.directory.trim());
            }

            for (String directory : directoriesList) {
                File folder = new File(directory.trim());
                folder.mkdir();
                File f = new File(directory.trim() + "/" + transactionInfo.dateTimeCode + ".csv");
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(f), "windows-1251"));
                for (ItemInfo item : transactionInfo.itemsList) {
                    String row = item.idBarcode + ";" + item.name + ";" + item.price;
                    writer.println(row);
                }
                writer.close();
            }
        }
    }
}
