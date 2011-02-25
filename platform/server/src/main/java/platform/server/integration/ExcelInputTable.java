package platform.server.integration;

import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: DAle
 * Date: 24.02.11
 * Time: 15:43
 */

public class ExcelInputTable implements ImportInputTable {
    private jxl.Sheet sheet;

    public ExcelInputTable(File file) throws BiffException, IOException {
        this(file, 0);
    }

    public ExcelInputTable(File file, int sheetNumber) throws BiffException, IOException {
        sheet = Workbook.getWorkbook(file).getSheet(sheetNumber);
    }


    public ExcelInputTable(InputStream stream) throws BiffException, IOException {
        this(stream, 0);
    }

    public ExcelInputTable(InputStream stream, int sheetNumber) throws BiffException, IOException {
        sheet = Workbook.getWorkbook(stream).getSheet(sheetNumber);
    }

    public String getCellString(int row, int column) {
        return sheet.getCell(column, row).getContents();
    }

    public int rowsCnt() {
        return sheet.getRows();
    }

    public int columnsCnt() {
        return sheet.getColumns();
    }

}
