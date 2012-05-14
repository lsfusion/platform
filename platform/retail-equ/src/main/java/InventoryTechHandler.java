import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.indexes.Index;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import retail.api.remote.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryTechHandler extends TerminalHandler<InventoryTechSalesBatch> {

    public InventoryTechHandler() {
    }

    @Override
    public void sendTransaction(TransactionTerminalInfo transactionInfo, List<TerminalInfo> machineryInfoList) throws IOException {

        DBF fileBarcode = null;
        Index fileBarcodeIndex = null;
        DBF fileGoods = null;
        Index fileGoodsIndex = null;

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

                fileBarcode = new DBF(directory + "/BARCODE.DBF", "CP866");

                if (transactionInfo.snapshot) {
                    for (int i = 0; i < fileBarcode.getRecordCount(); i++) {
                        fileBarcode.gotoRecord(i + 1);
                        fileBarcode.delete();
                    }
                    fileBarcode.pack();
                }
                fileBarcodeIndex = fileBarcode.createIndex(directory + "/" + transactionInfo.dateTimeCode + "B.NDX", "BARCODE", true, true);

                for (ItemInfo item : transactionInfo.itemsList) {
                    if (fileBarcode.findExact(item.barcodeEx.trim())) {
                        fileBarcode.getField("ARTICUL").put(item.barcodeEx);
                        fileBarcode.getField("BARCODE").put(item.barcodeEx);
                        fileBarcode.update();
                    } else {
                        if (fileBarcode.getRecordCount() != 0)
                            fileBarcode.gotoRecord(fileBarcode.getRecordCount());
                        fileBarcode.getField("ARTICUL").put(item.barcodeEx);
                        fileBarcode.getField("BARCODE").put(item.barcodeEx);
                        fileBarcode.write();
                        fileBarcode.file.setLength(fileBarcode.file.length() - 1);
                    }
                }

                if (!new File(directory + "/GOODS.MDX").exists()) {
                    File fileGoodsCDX = new File(directory + "/GOODS.CDX");
                    fileGoodsCDX.renameTo(new File(directory + "/GOODS.MDX"));
                }

                fileGoods = new DBF(directory + "/GOODS.DBF", "CP866");

                if (transactionInfo.snapshot) {
                    for (int i = 0; i < fileGoods.getRecordCount(); i++) {
                        fileGoods.gotoRecord(i + 1);
                        fileGoods.delete();
                    }
                    fileGoods.pack();
                }
                fileGoodsIndex = fileGoods.createIndex(directory + "/" + transactionInfo.dateTimeCode + "G.NDX", "ARTICUL", true, true);

                for (ItemInfo item : transactionInfo.itemsList) {
                    if (fileGoods.findExact(item.barcodeEx.trim())) {
                        fileGoods.getField("ARTICUL").put(item.barcodeEx);
                        fileGoods.getField("NAME").put(item.name);
                        fileGoods.getField("PRICE").put(item.price.toString());
                        fileGoods.update();
                    } else {
                        fileGoods.getField("ARTICUL").put(item.barcodeEx);
                        fileGoods.getField("NAME").put(item.name);
                        fileGoods.getField("PRICE").put(item.price.toString());
                        fileGoods.write();
                        fileGoods.file.setLength(fileGoods.file.length() - 1);
                    }
                }
            }

            sendTerminalDocumentTypes(machineryInfoList, remote.readTerminalDocumentTypeInfo());

        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            fileBarcode.close();
            if (fileBarcodeIndex != null && !fileBarcodeIndex.file.delete())
                throw new RuntimeException("File" + fileBarcodeIndex.file.getAbsolutePath() + " can not be deleted");
            fileGoods.close();
            if (fileGoodsIndex != null && !fileGoodsIndex.file.delete())
                throw new RuntimeException("File" + fileGoodsIndex.file.getAbsolutePath() + " can not be deleted");
        }
    }

    @Override
    public void sendTerminalDocumentTypes(List<TerminalInfo> terminalInfoList,
                                          List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList) throws IOException {

        DBF fileSPRDOC = null;
        Index fileSPRDOCIndex = null;
        DBF fileSPRAV = null;
        Index fileSPRAVIndex = null;
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

                fileSPRDOC = new DBF(directory + "/SPRDOC.DBF", "CP866");
                fileSPRDOCIndex = fileSPRDOC.createIndex(directory + "/" + "SPRDOC.NDX", "CODE", true, true);

                for (TerminalDocumentTypeInfo docType : terminalDocumentTypeInfoList) {
                    String id = docType.id != null ? docType.id.trim() : "";
                    String name = docType.name != null ? docType.name.trim() : "";

                    String nameInHandbook1 = docType.nameInHandbook1 != null ? docType.nameInHandbook1.trim() : "";
                    String idTerminalHandbookType1 = docType.idTerminalHandbookType1 != null ? docType.idTerminalHandbookType1.trim() : "";
                    String nameInHandbook2 = docType.nameInHandbook2 != null ? docType.nameInHandbook2.trim() : "";
                    String idTerminalHandbookType2 = docType.idTerminalHandbookType2 != null ? docType.idTerminalHandbookType2.trim() : "";

                    if (fileSPRDOC.findExact(docType.id.trim())) {
                        fileSPRDOC.getField("CODE").put(id);
                        fileSPRDOC.getField("NAME").put(name);
                        fileSPRDOC.getField("SPRT1").put(nameInHandbook1);
                        fileSPRDOC.getField("VIDSPR1").put(idTerminalHandbookType1);
                        fileSPRDOC.getField("SPRT2").put(nameInHandbook2);
                        fileSPRDOC.getField("VIDSPR2").put(idTerminalHandbookType2);
                        fileSPRDOC.update();
                    } else {
                        fileSPRDOC.getField("CODE").put(id);
                        fileSPRDOC.getField("NAME").put(name);
                        fileSPRDOC.getField("SPRT1").put(nameInHandbook1);
                        fileSPRDOC.getField("VIDSPR1").put(idTerminalHandbookType1);
                        fileSPRDOC.getField("SPRT2").put(nameInHandbook2);
                        fileSPRDOC.getField("VIDSPR2").put(idTerminalHandbookType2);
                        fileSPRDOC.write();
                    }
                }

                if (!new File(directory + "/SPRAV.MDX").exists()) {
                    File fileSpravCDX = new File(directory + "/SPRAV.CDX");
                    fileSpravCDX.renameTo(new File((directory + "/SPRAV.MDX")));
                }

                fileSPRAV = new DBF(directory + "/SPRAV.DBF", "CP866");
                fileSPRAVIndex = fileSPRAV.createIndex(directory + "/" + "SPRAV.NDX", "CODE", true, true);

                for (TerminalDocumentTypeInfo docType : terminalDocumentTypeInfoList) {

                    List<LegalEntityInfo> legalEntityInfoList = docType.legalEntityInfoList;

                    for (LegalEntityInfo legalEntityInfo : legalEntityInfoList) {
                        String id = legalEntityInfo.id != null ? legalEntityInfo.id.trim() : "";
                        String name = legalEntityInfo.name != null ? legalEntityInfo.name.trim() : "";
                        String type = legalEntityInfo.type != null ? legalEntityInfo.type.trim() : "";
                        if (fileSPRAV.findExact(legalEntityInfo.id.trim())) {
                            fileSPRAV.getField("CODE").put(id);
                            fileSPRAV.getField("NAME").put(name);
                            fileSPRAV.getField("VIDSPR").put(type);
                            fileSPRAV.update();
                        } else {
                            fileSPRAV.getField("CODE").put(id);
                            fileSPRAV.getField("NAME").put(name);
                            fileSPRAV.getField("VIDSPR").put(type);
                            fileSPRAV.write();
                        }
                    }
                }
            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        } finally {
            fileSPRDOC.close();
            if (fileSPRDOCIndex != null && !fileSPRDOCIndex.file.delete())
                throw new RuntimeException("File" + fileSPRDOCIndex.file.getAbsolutePath() + " can not be deleted");
            fileSPRAV.close();
            if (fileSPRAVIndex != null && !fileSPRAVIndex.file.delete())
                throw new RuntimeException("File" + fileSPRAVIndex.file.getAbsolutePath() + " can not be deleted");

        }
    }

    @Override
    public List<TerminalDocumentInfo> readTerminalDocumentInfo(List<TerminalInfo> terminalInfoList) throws IOException {

        DBF importFilePos = null;
        DBF importFile = null;

        try {

            List<String> directoriesList = new ArrayList<String>();
            for (TerminalInfo terminalInfo : terminalInfoList) {
                if ((terminalInfo.port != null) && (!directoriesList.contains(terminalInfo.port.trim())))
                    directoriesList.add(terminalInfo.port.trim());
                if ((terminalInfo.directory != null) && (!directoriesList.contains(terminalInfo.directory.trim())))
                    directoriesList.add(terminalInfo.directory.trim());
            }
            List<TerminalDocumentInfo> terminalDocumentInfoList = new ArrayList<TerminalDocumentInfo>();
            List<TerminalDocumentDetailInfo> terminalDocumentDetailInfoList = new ArrayList<TerminalDocumentDetailInfo>();
            for (String directory : directoriesList) {

                Util.setxBaseJProperty("ignoreMissingMDX", "true");

                if (!new File(directory + "/POS.MDX").exists()) {
                    File filePOSCDX = new File(directory + "/POS.CDX");
                    filePOSCDX.renameTo(new File((directory + "/POS.MDX")));
                }

                importFilePos = new DBF(directory + "/Pos.DBF", "CP866");
                int recordCountPos = importFilePos.getRecordCount();

                for (int i = 0; i < recordCountPos; i++) {
                    importFilePos.read();
                    Integer id = new Integer(new String(importFilePos.getField("IDDOC").getBytes(), "Cp866").trim());
                    String barcode = new String(importFilePos.getField("ARTICUL").getBytes(), "Cp866").trim();
                    String name = new String(importFilePos.getField("NAME").getBytes(), "Cp866").trim();
                    Double quantity = new Double(new String(importFilePos.getField("QUAN").getBytes(), "Cp866").trim());
                    Double price = new Double(new String(importFilePos.getField("PRICE").getBytes(), "Cp866").trim());
                    Double sum = price * quantity;
                    Boolean isNew = barcode.contains("*") ? true : null;
                    terminalDocumentDetailInfoList.add(new TerminalDocumentDetailInfo(id, barcode.replace("*", ""), name, isNew, quantity, price, sum));
                }

                if (!new File(directory + "/DOC.MDX").exists()) {
                    File fileDOCCDX = new File(directory + "/DOC.CDX");
                    fileDOCCDX.renameTo(new File((directory + "/DOC.MDX")));
                }

                importFile = new DBF(directory + "/Doc.DBF", "CP866");
                int recordCount = importFile.getRecordCount();

                for (int i = 0; i < recordCount; i++) {
                    importFile.read();
                    Integer accepted = new Integer(new String(importFile.getField("ACCEPTED").getBytes(), "Cp866").trim());
                    if (accepted == 0) {
                        Integer id = new Integer(new String(importFile.getField("IDDOC").getBytes(), "Cp866").trim());
                        String type = new String(importFile.getField("CVIDDOC").getBytes(), "Cp866").trim();
                        String handbook1 = new String(importFile.getField("CSPR1").getBytes(), "Cp866").trim();
                        String handbook2 = new String(importFile.getField("CSPR2").getBytes(), "Cp866").trim();
                        String title = new String(importFile.getField("TITLE").getBytes(), "Cp866").trim();
                        Double quantity = new Double(new String(importFile.getField("QUANDOC").getBytes(), "Cp866").trim());
                        List<TerminalDocumentDetailInfo> currentTerminalDocumentDetailInfoList = new ArrayList<TerminalDocumentDetailInfo>();
                        for (TerminalDocumentDetailInfo terminalDocumentDetailInfo : terminalDocumentDetailInfoList) {
                            if (terminalDocumentDetailInfo.id.equals(id))
                                currentTerminalDocumentDetailInfoList.add(terminalDocumentDetailInfo);
                        }
                        terminalDocumentInfoList.add(new TerminalDocumentInfo(id, type,
                                "".equals(handbook1) ? null : new Integer(handbook1), "".equals(handbook2) ? null : new Integer(handbook2),
                                title, quantity, currentTerminalDocumentDetailInfoList));
                    }
                }
            }
            return terminalDocumentInfoList;

        } catch (xBaseJException e) {
            throw new RuntimeException(e.toString(), e.getCause());
        } finally {
            importFilePos.close();
            importFile.close();
        }
    }

    @Override
    public void finishSendingTerminalDocumentInfo(List<TerminalInfo> terminalInfoList) throws IOException {

        DBF fileDOC = null;
        Index fileDOCIndex = null;

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

                if (!new File(directory + "/DOC.MDX").exists()) {
                    File fileDOCCDX = new File(directory + "/DOC.CDX");
                    fileDOCCDX.renameTo(new File((directory + "/DOC.MDX")));
                }

                fileDOC = new DBF(directory + "/DOC.DBF", "CP866");
                int recordCount = fileDOC.getRecordCount();

                fileDOCIndex = fileDOC.createIndex(directory + "/" + "DOC.NDX", "IDDOC", true, true);

                for (int i = 1; i <= recordCount; i++) {
                    fileDOC.gotoRecord(i);
                    fileDOC.getField("ACCEPTED").put("1");
                    fileDOC.update();
                }
            }
        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            fileDOC.close();
            if (fileDOCIndex != null && !fileDOCIndex.file.delete())
                throw new RuntimeException("File" + fileDOCIndex.file.getAbsolutePath() + " can not be deleted");
        }
    }
}
