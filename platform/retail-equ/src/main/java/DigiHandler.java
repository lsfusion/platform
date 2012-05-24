import platform.base.IOUtils;
import retail.api.remote.*;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DigiHandler extends ScalesHandler {

    public DigiHandler() {
    }

    @Override
    public void sendTransaction(TransactionScalesInfo transactionInfo, List<ScalesInfo> machineryInfoList) throws IOException {

        List<String> directoriesList = new ArrayList<String>();
        List<String> scalesModelsList = new ArrayList<String>();
        for (ScalesInfo scalesInfo : machineryInfoList) {
            if ((scalesInfo.port != null) && (!directoriesList.contains(scalesInfo.port.trim())))
                directoriesList.add(scalesInfo.port.trim());
            if ((scalesInfo.directory != null) && (!directoriesList.contains(scalesInfo.directory.trim())))
                directoriesList.add(scalesInfo.directory.trim());
            if ((scalesInfo.nameModel != null) && (!scalesModelsList.contains(scalesInfo.nameModel.trim())))
                scalesModelsList.add(scalesInfo.nameModel.trim());
        }

        for (String directory : directoriesList) {

            if (transactionInfo.snapshot) {
                try {

                    for (byte[][] fileLabelFormat : remote.readLabelFormats(scalesModelsList)) {

                        File file34 = new File(directory + "/SM090F34.DAT");
                        File file38 = new File(directory + "/SM090F38.DAT");

                        FileOutputStream fileOutputStream = new FileOutputStream(file34);
                        fileOutputStream.write(fileLabelFormat[0], 4, fileLabelFormat[0].length - 4);
                        fileOutputStream.close();

                        if (fileLabelFormat[1] != null) {
                            fileOutputStream = new FileOutputStream(file38);
                            fileOutputStream.write(fileLabelFormat[1], 4, fileLabelFormat[1].length - 4);
                            fileOutputStream.close();
                        }

                        deleteErrorFiles(directory);
                        Runtime.getRuntime().exec(new String[]{directory + "/TWSWTCP.EXE", "F34.DAT", "090"}, null, new File(directory)).waitFor();
                        checkErrorFiles(directory);

                        if (file34.exists() && !file34.delete())
                            throw new RuntimeException("File" + file34.getAbsolutePath() + " can not be deleted");
                        if (file38.exists() && !file38.delete())
                            throw new RuntimeException("File" + file38.getAbsolutePath() + " can not be deleted");

                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (SQLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            File folder = new File(directory.trim());
            folder.mkdir();
            File f = new File(directory.trim() + "/SM090F25.DAT");
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(f), "CP866"));
            String row = "";
            for (ItemInfo item : transactionInfo.itemsList) {

                String recordNumber = addZeros(item.barcodeEx, 8, false);
                String statusCode = "7C000DA003"; //54000DA003
                String price = addZeros(String.valueOf(item.price.intValue()), 8, false);
                String daysExpiry = addZeros(String.valueOf(item.daysExpiry == null ? null : item.daysExpiry.intValue()), 4, false);
                String hoursExpiry = addZeros(addZeros(String.valueOf(item.hoursExpiry), 2, false), 4, true);
                String labelFormat = addZeros(Integer.toHexString(item.labelFormat != null ? item.labelFormat : 0), 2, false);

                String barcodeFormat = "05";

                String barcode = "20" + item.barcodeEx + "0000001";
                String len = addZeros(Integer.toHexString((recordNumber + statusCode + price + labelFormat + barcodeFormat +
                        barcode + daysExpiry + hoursExpiry +
                        itemNameCompositionToASCII(item.name, item.composition) + "0C00").length() / 2 + 2), 4, false).toUpperCase();

                row += recordNumber + len + statusCode + price + labelFormat + barcodeFormat + barcode +
                        daysExpiry + hoursExpiry + itemNameCompositionToASCII(item.name, item.composition) + "0C00";
            }
            writer.print(row);
            writer.close();

            deleteErrorFiles(directory);

            try {
                Runtime.getRuntime().exec(new String[]{directory + "/TWSWTCP.EXE", "F25.DAT", "090"}, null, new File(directory)).waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            checkErrorFiles(directory);
        }

    }

    private String addZeros(String str, Integer len, Boolean toTheEnd) {
        if (str == null)
            str = "";
        while (str.length() < len) {
            if (toTheEnd)
                str += "0";
            else
                str = "0" + str;
        }
        return str;
    }

    private String itemNameCompositionToASCII(String itemName, String itemComposition) throws UnsupportedEncodingException {
        String outputString = "04" + addZeros(Integer.toHexString(itemName.length()), 2, false).toUpperCase();
        for (byte b : itemName.getBytes("Cp866")) {
            int code = Integer.valueOf(b);
            outputString += Integer.toHexString(code < 0 ? code + 256 : code).toUpperCase();
        }
        if (itemComposition != null) {
            outputString += "0C" + "02" + addZeros(Integer.toHexString(itemComposition.length()), 2, false).toUpperCase();
            for (byte b : itemComposition.getBytes("Cp866")) {
                int code = Integer.valueOf(b);
                outputString += Integer.toHexString(code < 0 ? code + 256 : code).toUpperCase();
            }
        }
        return outputString;
    }

    private void deleteErrorFiles(String directory) {
        File errorFile = new File(directory + "/error");
        if (errorFile.exists() && !errorFile.delete())
            throw new RuntimeException("File" + errorFile.getAbsolutePath() + " can not be deleted");
        File retvalsFile = new File(directory + "/retvals");
        if (retvalsFile.exists() && !retvalsFile.delete())
            throw new RuntimeException("File" + retvalsFile.getAbsolutePath() + " can not be deleted");
    }

    private void checkErrorFiles(String directory) throws IOException {
        String errorString = "";
        File errorFile = new File(directory + "/error");
        if (errorFile.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(errorFile));
            String str;
            while ((str = in.readLine()) != null) {
                errorString += str + " ";
            }
            in.close();
        }
        File retvalsFile = new File(directory + "/retvals");
        if (retvalsFile.exists()) {
            errorString += "Return values: ";
            BufferedReader in = new BufferedReader(new FileReader(retvalsFile));
            String str;
            while ((str = in.readLine()) != null) {
                errorString += str + " ";
            }
            in.close();
        }
        if (!"0".equals(errorString.trim()))
            throw new RuntimeException("Scales Error: " + errorString);
    }
}
