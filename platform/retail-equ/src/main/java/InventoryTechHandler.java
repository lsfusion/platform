import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.indexes.Index;
import org.xBaseJ.xBaseJException;
import retail.api.remote.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class InventoryTechHandler extends TerminalHandler<InventoryTechSalesBatch> {

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

                if (!new File(directory + "/BARCODE.MDX").exists()) {
                    File fileBarcodeCDX = new File(directory + "/BARCODE.CDX");
                    fileBarcodeCDX.renameTo(new File((directory + "/BARCODE.MDX")));
                }

                DBF fileBarcode = new DBF(directory + "/BARCODE.DBF", "CP866");

                if (transactionInfo.snapshot) {
                    for (int i = 0; i < fileBarcode.getRecordCount(); i++) {
                        fileBarcode.gotoRecord(i + 1);
                        fileBarcode.delete();
                    }
                    fileBarcode.pack();
                }
                Index fileBarcodeIndex = fileBarcode.createIndex(directory + "/" + transactionInfo.dateTimeCode + "B.NDX", "BARCODE", true, true);

                for (ItemInfo item : transactionInfo.itemsList) {
                    if (fileBarcode.findExact(item.barcodeEx)) {
                        fileBarcode.getField("ARTICUL").put(item.barcodeEx);
                        fileBarcode.getField("BARCODE").put(item.barcodeEx);
                        fileBarcode.update();
                    } else {
                        if (fileBarcode.getRecordCount() != 0)
                            fileBarcode.gotoRecord(fileBarcode.getRecordCount());
                        fileBarcode.getField("ARTICUL").put(item.barcodeEx);
                        fileBarcode.getField("BARCODE").put(item.barcodeEx);
                        fileBarcode.write();
                        fileBarcode.file.setLength(fileBarcode.file.length()-1);
                    }
                }
                fileBarcode.close();
                if (!fileBarcodeIndex.file.delete())
                    throw new RuntimeException("File" + fileBarcodeIndex.file.getAbsolutePath() + " can not be deleted");

                if (!new File(directory + "/GOODS.MDX").exists()) {
                    File fileGoodsCDX = new File(directory + "/GOODS.CDX");
                    fileGoodsCDX.renameTo(new File(directory + "/GOODS.MDX"));
                }

                DBF fileGoods = new DBF(directory + "/GOODS.DBF", "CP866");

                if (transactionInfo.snapshot) {
                    for (int i = 0; i < fileGoods.getRecordCount(); i++) {
                        fileGoods.gotoRecord(i + 1);
                        fileGoods.delete();
                    }
                    fileGoods.pack();
                }
                Index fileGoodsIndex = fileGoods.createIndex(directory + "/" + transactionInfo.dateTimeCode + "G.NDX", "ARTICUL", true, true);

                for (ItemInfo item : transactionInfo.itemsList) {
                    if (fileGoods.findExact(item.barcodeEx)) {
                        fileGoods.getField("ARTICUL").put(item.barcodeEx);
                        fileGoods.getField("NAME").put(item.name);
                        fileGoods.getField("PRICE").put(item.price.toString());
                        fileGoods.update();
                    } else {
                        if (fileGoods.getRecordCount() != 0)
                            fileGoods.gotoRecord(fileGoods.getRecordCount());
                        fileGoods.getField("ARTICUL").put(item.barcodeEx);
                        fileGoods.getField("NAME").put(item.name);
                        fileGoods.getField("PRICE").put(item.price.toString());
                        fileGoods.write();
                        fileGoods.file.setLength(fileGoods.file.length()-1);
                    }
                }
                fileGoods.close();
                if (!fileGoodsIndex.file.delete())
                    throw new RuntimeException("File" + fileGoodsIndex.file.getAbsolutePath() + " can not be deleted");
                fileGoodsIndex.file = null;
            }

            sendTerminalDocumentTypes(machineryInfoList, remote.readTerminalDocumentTypeInfo());

        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void sendTerminalDocumentTypes(List<TerminalInfo> terminalInfoList,
                                          List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList) throws IOException {
        try {

            List<String> directoriesList = new ArrayList<String>();
            for (TerminalInfo terminalInfo : terminalInfoList) {
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

                if (!new File(directory + "/SPRDOC.MDX").exists()) {
                    File fileBarcodeCDX = new File(directory + "/SPRDOC.CDX");
                    fileBarcodeCDX.renameTo(new File((directory + "/SPRDOC.MDX")));
                }

                DBF fileSPRDOC = new DBF(directory + "/SPRDOC.DBF", "CP866");

                Index fileSPRDOCIndex = fileSPRDOC.createIndex(directory + "/" + "SPRDOC.NDX", "CODE", true, true);

                for (TerminalDocumentTypeInfo docType : terminalDocumentTypeInfoList) {
                    String id = docType.id!=null ? docType.id.trim() : "";
                    String name = docType.name!=null ? docType.name.trim() : "";
                    String groupName = docType.groupName!=null ? docType.groupName.trim() : "";
                    if (fileSPRDOC.findExact(docType.id)) {
                        fileSPRDOC.getField("CODE").put(id);
                        fileSPRDOC.getField("NAME").put(name);
                        fileSPRDOC.getField("SPRT1").put(groupName);
                        fileSPRDOC.update();
                    } else {
                        if (fileSPRDOC.getRecordCount() != 0)
                            fileSPRDOC.gotoRecord(fileSPRDOC.getRecordCount());
                        fileSPRDOC.getField("CODE").put(id);
                        fileSPRDOC.getField("NAME").put(name);
                        fileSPRDOC.getField("SPRT1").put(groupName);
                        fileSPRDOC.write();
                    }
                }
                fileSPRDOC.close();
                if (!fileSPRDOCIndex.file.delete())
                    throw new RuntimeException("File" + fileSPRDOCIndex.file.getAbsolutePath() + " can not be deleted");
            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        }
    }
}
