package platform.server.integration;

import jxl.*;
import jxl.read.biff.BiffException;
import platform.server.classes.DateClass;
import platform.server.classes.DoubleClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

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

    public ExcelInputTable(jxl.Sheet sheet) {
        this.sheet = sheet;
    }

    public String getCellString(int row, int column) {
        return sheet.getCell(column, row).getContents();
    }

    public String getCellString(ImportField field, int row, int column) throws ParseException {
        Cell cell = sheet.getCell(column, row);
        if (field.getType() == DoubleClass.instance && cell.getType() == CellType.NUMBER) {
            return String.valueOf(((NumberCell) cell).getValue());
        } else if (field.getType() == DateClass.instance && cell.getType() == CellType.DATE) {
            Date date = ((DateCell) cell).getDate();
            DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format.format(date);
        } else {
            return getCellString(row, column);
        }
    }

    @Override
    public String getCellVal(int row, int column) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getCellVal(ImportField field, int row, int column) throws ParseException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int rowsCnt() {
        return sheet.getRows();
    }

    public int columnsCnt() {
        return sheet.getColumns();
    }

}
