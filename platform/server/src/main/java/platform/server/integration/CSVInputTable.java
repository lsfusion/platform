package platform.server.integration;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: DAle
 * Date: 28.02.11
 * Time: 17:14
 */

public class CSVInputTable implements ImportInputTable {
    private List<List<String>> data = new ArrayList<List<String>>();

    public CSVInputTable(java.io.Reader reader, int headerLines, int delimiterChar) throws IOException {
        CsvListReader csvReader = new CsvListReader(reader, new CsvPreference('"', delimiterChar, "\n"));
        for (int i = 0; i < headerLines; i++) {
            csvReader.getCSVHeader(false);
        }

        while (true) {
            List<String> row = csvReader.read();
            if (row == null) break;
            data.add(new ArrayList<String>(row));
        }
    }

    public String getCellString(int row, int column) {
        return data.get(row).get(column);
    }

    public int rowsCnt() {
        return data.size();
    }

    public int columnsCnt() {
        return data.get(0).size();
    }
}
