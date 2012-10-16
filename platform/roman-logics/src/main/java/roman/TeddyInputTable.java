package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class TeddyInputTable implements ImportInputTable {
    private List<List<String>> detailData = new ArrayList<List<String>>();
    private Map<String, List<String>> mergeData = new HashMap<String, List<String>>();

    public TeddyInputTable(ByteArrayInputStream stream) throws IOException {

        ZipInputStream zin = new ZipInputStream(stream);
        ZipEntry entry;

        while ((entry = zin.getNextEntry()) != null) {
            String name = entry.getName();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int readCount;
            byte[] buffer = new byte[(int) entry.getSize()];
            while ((readCount = zin.read(buffer, 0, buffer.length)) != -1) {
                output.write(buffer, 0, readCount);
            }
            if (name.endsWith("-d.txt")) {
                Scanner scanner = new Scanner(new ByteArrayInputStream(output.toByteArray()), "unicode");
                int i = 0;
                String invoiceNumber = "";
                String date = "";
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (i == 7) {
                        invoiceNumber = line.substring(21, 30);
                        date = line.substring(38, 48);
                    }
                    i++;
                    if (isCorrectDetailRow(line)) {
                        List<String> row = new ArrayList<String>();
                        row.add(invoiceNumber);
                        row.add(date);
                        row.add(line.substring(0, 12));//barcode
                        row.add(line.substring(13, 16) + line.substring(17, 24));//articleNumber
                        row.add(line.substring(41, 44));//brandSID
                        String fullDescription = line.substring(45, 68);
                        String[] desc = fullDescription.split(" ");
                        String description = "";
                        for (int j = 0; j < desc.length - 1; j++)
                            description += desc[j];
                        row.add(description);//description & subcategory
                        row.add(desc[desc.length - 1]);//gender
                        row.add(line.substring(68, 72));//colorSID
                        row.add(line.substring(73, 84));//colorName
                        row.add(line.substring(84, 91));//size
                        row.add(line.substring(93, 102));//quantity
                        row.add(line.substring(103, 117));//price
                        row.add(line.substring(164, 184));//rrp
                        detailData.add(row);
                    }
                }
            } else if (name.matches("[0-9/]+\\.txt")) {
                Scanner scanner = new Scanner(new ByteArrayInputStream(output.toByteArray()), "unicode");
                String articleNumber = ""; //articleNumber
                String composition = "";  //composition
                String netWeight = "";    //netWeight
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (isArticleRow(line)) {
                        articleNumber = line.substring(2, 5) + line.substring(6, 13);
                    } else if (isCompositionRow(line)) {
                        composition = line.trim().substring((3));
                    } else if (isNetWeightRow(line)) {
                        netWeight = line.trim().substring(13);
                    } else if (isCountryCustomCategoryRow(line)) {
                        String customCategory = line.trim().substring(22, 30);
                        //while (customCategory.length() < 10)
                        //    customCategory = "0" + customCategory;
                        String country = line.trim().substring(46);

                        mergeData.put(articleNumber,
                                new ArrayList<String>(Arrays.asList(composition, netWeight, customCategory, country)));
                    }
                }
            }
        }

        for (List<String> row : detailData) {
            if (mergeData.containsKey(row.get(3)))
                row.addAll(mergeData.get(row.get(3)));
        }
    }

    public String getCellString(int row, int column) {
        return detailData.get(row).get(column);
    }

    public String getCellString(ImportField field, int row, int column) {
        return getCellString(row, column);
    }

    public int rowsCnt() {
        return detailData.size();
    }

    public int columnsCnt() {
        return detailData.get(0).size();
    }

    protected boolean isCorrectDetailRow(String row) {
        return row.matches("\\d{12}.*");
    }

    protected boolean isArticleRow(String row) {
        return row.matches("\\s{2}\\w{3}\\s\\d{7}.*");
    }

    protected boolean isCompositionRow(String row) {
        return row.trim().matches("^\\.:\\s-.*$");
    }

    protected boolean isNetWeightRow(String row) {
        return row.trim().startsWith("Net Weight");
    }

    protected boolean isCountryCustomCategoryRow(String row) {
        return row.trim().startsWith("Taric");
    }
}
