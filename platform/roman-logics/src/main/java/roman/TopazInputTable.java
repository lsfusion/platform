package roman;

import jxl.Workbook;
import jxl.read.biff.BiffException;
import platform.server.integration.ExcelInputTable;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.server.integration.SingleSheetImporter.*;

public class TopazInputTable implements ImportInputTable {
    private List<List<String>> data = new ArrayList<List<String>>();

    public final static int barcodeColumn = B;
    public final static int lastColumn = G;

    public TopazInputTable(InputStream stream) throws BiffException, IOException {
        Workbook book = Workbook.getWorkbook(stream);

        ExcelInputTable invoiceTable = new ExcelInputTable(book.getSheet(0));

        String invoiceSID = invoiceTable.getCellString(2, A).trim();

        for (int row = 3; row < invoiceTable.rowsCnt(); row++) {
            String articleID = invoiceTable.getCellString(row, A).trim();
            if (articleID.length() > 0) {
                List<String> rowData = new ArrayList<String>();
                for (int column = 0; column <= lastColumn; column++) {
                    String cellValue = invoiceTable.getCellString(row, column);
                    if (column == 0)
                        rowData.add(transformValue(cellValue));
                    else
                        rowData.add(cellValue);
                }
                rowData.add(invoiceSID);
                rowData.add(invoiceSID);
                rowData.add("" + (row - 3));
                data.add(rowData);
            }
        }
    }

    @Override
    public String getCellString(int row, int column) {
        assert row < rowsCnt() && column < columnsCnt();
        return data.get(row).get(column);
    }

    @Override
    public String getCellString(ImportField field, int row, int column) throws ParseException {
        return getCellString(row, column);
    }

    @Override
    public int rowsCnt() {
        return data.size();
    }

    @Override
    public int columnsCnt() {
        if (data.size() == 0) return 0;
        return data.get(0).size();
    }
    
    public String transformValue(String value) {
        String [] splitValue = value.split("\\.");
        return splitValue[0];
    }
}
