package roman;

import jxl.read.biff.BiffException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import platform.server.classes.DateClass;
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

/**
 * User: DAle
 * Date: 16.06.11
 * Time: 14:03
 */


// Получаем таблицу в которой сначала идут столбцы invoice, потом столбец с invoiceNumber, потом столбцы из PL
public class WomenSecretInputTable implements ImportInputTable {
    private List<List<String>> data = new ArrayList<List<String>>();

    public final static int lastMainColumn = I;
    public final static int lastInvoiceColumn = L;
    public final static int resultBarcodeColumn = lastInvoiceColumn + 3 + lastMainColumn;

    public WomenSecretInputTable(InputStream stream) throws BiffException, IOException, InvalidFormatException {
        try {
            Workbook book = WorkbookFactory.create(stream);
            int sheetNumber = book.getNumberOfSheets();
            for (int i = 0; i * 2 < sheetNumber; i++) {
                ExcelInputTable invoiceTable = new ExcelInputTable(book.getSheetAt(i * 2));
                ExcelInputTable mainTable = new ExcelInputTable(book.getSheetAt(i * 2 + 1));

                String invoiceID = invoiceTable.getCellString(6, B);
                String invoiceDate = invoiceTable.getCellString(new ImportField(DateClass.instance), 7, B);

                Map<String, List<String>> invoiceData = new HashMap<String, List<String>>();
                boolean startReading = false;
                for (int row = 0; row < invoiceTable.rowsCnt(); row++) {
                    final int ARTICLE_ID_COL = A;
                    String articleID = invoiceTable.getCellString(row, ARTICLE_ID_COL).trim();
                    if (articleID.equals("STYLE")) {
                        startReading = true;
                    }
                    if (articleID.length() > 0 && !articleID.equals("STYLE") && startReading) {
                        List<String> rowData = new ArrayList<String>();
                        for (int column = 0; column <= lastInvoiceColumn; column++) {
                            rowData.add(invoiceTable.getCellString(row, column));
                        }
                        invoiceData.put(articleID, rowData);
                    }
                }

                for (int row = 0; row < mainTable.rowsCnt(); row++) {
                    final int BARCODE_COL = I;
                    final int ARTICLE_ID_COL = B;
                    if (mainTable.getCellString(row, BARCODE_COL).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$")) {
                        String articleID = mainTable.getCellString(row, ARTICLE_ID_COL).trim();
                        List<String> invoiceRow = invoiceData.get(articleID);
                        List<String> rowData = new ArrayList<String>();
                        for (int column = 0; column <= lastInvoiceColumn; column++) {
                            rowData.add(invoiceRow == null ? "" : invoiceRow.get(column));
                        }
                        rowData.add(invoiceID);
                        rowData.add(invoiceDate);
                        for (int column = 0; column <= lastMainColumn; column++) {
                            rowData.add(mainTable.getCellString(row, column));
                        }
                        data.add(rowData);
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
}
