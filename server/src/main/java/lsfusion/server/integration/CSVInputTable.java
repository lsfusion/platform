package lsfusion.server.integration;

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
    Object[] columnsToRead;
    boolean readAll = true;

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

    public CSVInputTable(java.io.Reader reader, int headerLines, int delimiterChar, boolean readAll, Object... columnsToRead) throws IOException {
        this(reader, headerLines, delimiterChar);
        this.readAll = readAll;
        this.columnsToRead = columnsToRead;
    }

    public String getCellString(int row, int column) {
        return data.get(row).get(readAll ? column : (Integer) columnsToRead[column]);
    }

    public String getCellString(ImportField field, int row, int column) {
        return getCellString(row, column);
    }

    public int rowsCnt() {
        return data.size();
    }

    public int columnsCnt() {
        return readAll ? data.get(0).size() : columnsToRead.length;
    }
}
