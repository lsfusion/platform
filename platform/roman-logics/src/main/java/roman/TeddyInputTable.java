package roman;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import platform.base.BaseUtils;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class TeddyInputTable implements ImportInputTable {
    private List<List<String>> data = new ArrayList<List<String>>();

    public TeddyInputTable(ByteArrayInputStream stream) throws IOException {
        Scanner scanner = new Scanner(stream, "unicode");
        int i = 0;
        String invoiceNumber = "";
        String date = "";
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            if(i==7){
                invoiceNumber = line.substring(21,30);
                date = line.substring(38,48);
            }
            i++;
            if(isCorrectRow(line)){
                List<String> row = new ArrayList<String>();
                row.add(invoiceNumber);
                row.add(date);
                row.add(line.substring(0,12));//barcode
                row.add(line.substring(13, 16) + line.substring(17,24));//articleNumber
                row.add(line.substring(41,44));//brandSID
                String fullDescription = line.substring(45,68);
                String[] desc = fullDescription.split(" ");
                String description = "";
                for(int j=0;j<desc.length-1;j++)
                    description+=desc[j];
                row.add(description);//description & subcategory
                row.add(desc[desc.length-1]);//gender
                row.add(line.substring(68,72));//colorSID
                row.add(line.substring(73,84));//colorName
                row.add(line.substring(84,91));//size
                row.add(line.substring(93,102));//quantity
                row.add(line.substring(103,117));//price
                row.add(line.substring(164,184));//rrp
                data.add(row);
            }                
        }              
    }

    public String getCellString(int row, int column) {
        return data.get(row).get(column);
    }

    public String getCellString(ImportField field, int row, int column) {
        return getCellString(row, column);
    }

    public int rowsCnt() {
        return data.size();
    }

    public int columnsCnt() {
        return data.get(0).size();
    }

    protected boolean isCorrectRow(String row) {
        return row.matches("\\d{12}.*");
    }
}
