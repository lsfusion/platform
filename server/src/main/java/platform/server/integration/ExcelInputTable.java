package platform.server.integration;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.xml.sax.SAXException;
import platform.server.classes.DateClass;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;

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
                    return new DecimalFormat("#.######").format(cell.getNumericCellValue());
                else
                    return cell.getStringCellValue();
            } else
                return "";
        }
        return "";
    }

    public String getCellString(ImportField field, int row, int column) throws ParseException {
        Cell cell = sheet.getRow(row).getCell(column);
        if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
            if (field.getType() != DateClass.instance) {
                return new DecimalFormat("#.######").format(cell.getNumericCellValue());
            } else {
                return DateClass.getDateFormat().format(cell.getDateCellValue());
            }
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
