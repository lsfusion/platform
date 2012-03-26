package platform.server.integration;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import platform.server.classes.DateClass;
import platform.server.classes.DoubleClass;

import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import org.xml.sax.SAXException;

/**
 * User: DAle
 * Date: 24.02.11
 * Time: 15:43
 */

public class ExcelInputTable implements ImportInputTable {
    private Sheet sheet;

    public ExcelInputTable(InputStream stream) throws IOException, OpenXML4JException, SAXException {
        this(stream, 0);
    }

    public ExcelInputTable(InputStream stream, int sheetNumber) throws IOException, OpenXML4JException, SAXException {
        sheet = WorkbookFactory.create(stream).getSheetAt(sheetNumber);
    }

    public ExcelInputTable(Sheet sheet) {
        this.sheet = sheet;
    }

    public String getCellString(int row, int column) {
        if (sheet.getRow(row) != null) {
            Cell cell = sheet.getRow(row).getCell(column);
            if (cell != null) {
                if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC)
                    return new DecimalFormat("#").format(sheet.getRow(row).getCell(column).getNumericCellValue());
                else
                    return sheet.getRow(row).getCell(column).getStringCellValue();
            } else
                return "";
        }
        return "";
    }

    public String getCellString(ImportField field, int row, int column) throws ParseException {
        Cell cell = sheet.getRow(row).getCell(column);
        if (field.getType() == DoubleClass.instance && cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
            return new DecimalFormat("#").format(sheet.getRow(row).getCell(column).getNumericCellValue());
        } else if (field.getType() == DateClass.instance && cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
            Date date = cell.getDateCellValue();
            DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format.format(date);
        } else {
            return getCellString(row, column);
        }
    }

    public int rowsCnt() {
        return sheet.getPhysicalNumberOfRows();
    }

    public int columnsCnt() {
        return sheet.getRow(0).getPhysicalNumberOfCells();
    }

}
