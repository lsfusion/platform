package roman;

import jxl.read.biff.BiffException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static platform.server.integration.SingleSheetImporter.*;

public class DieselOrderInputTable implements ImportInputTable {
    Sheet sheet;
    private List<List<String>> data = new ArrayList<List<String>>();

    public DieselOrderInputTable(InputStream stream) throws BiffException, IOException, InvalidFormatException {

        sheet = WorkbookFactory.create(stream).getSheetAt(0);
        List<String> row;

        for (int i = 7; i < sheet.getPhysicalNumberOfRows(); i++) {
            row = new ArrayList<String>();
            row.add(getCellStringValue(i, A));  //season
            row.add(getCellStringValue(i, C));  //gender
            row.add(String.valueOf(getCellNumericValue(i, D)));  //orderSID
            row.add(getCellStringValue(i, E));  //date
            row.add(getCellStringValue(i, J));  //customCode
            row.add(getCellStringValue(i, K));  //composition
            row.add(getCellStringValue(i, O));  //article
            row.add(getCellStringValue(i, P));  //name
            row.add(getCellStringValue(i, Q));  //colorCode
            row.add(getCellStringValue(i, R));  //country
            row.add(String.valueOf(getCellNumericValue(i, U)));  //price
            row.add(sheet.getRow(i).getCell(V).getStringCellValue());  //dateFrom
            row.add(sheet.getRow(i).getCell(W).getStringCellValue());  //dateTo

            for (int j = AB; j <= AS; j++) {
                List<String> tempRow = new ArrayList<String>();
                for (String str : row)
                    tempRow.add(str);
                if (getCellNumericValue(i, j) != 0) {
                    String code = getCellStringValue(i, AA); //sizeField
                    if ("01".equals(code))
                        tempRow.add(getCellStringValue(0, j));
                    else if ("04".equals(code))
                        tempRow.add(getCellStringValue(1, j));
                    else if ("05".equals(code))
                        tempRow.add(getCellStringValue(2, j));
                    else if ("25".equals(code))
                        tempRow.add(getCellStringValue(3, j));
                    else if ("33".equals(code))
                        tempRow.add(getCellStringValue(4, j));
                    else if ("36".equals(code))
                        tempRow.add(getCellStringValue(5, j));
                    if (sheet.getRow(i).getCell(j) != null)
                        tempRow.add(String.valueOf(getCellNumericValue(i, j))); //unitQuantityField

                    data.add(tempRow);
                }
            }
        }
    }

    @Override
    public String getCellString(int row, int column) {
        return data.get(row).get(column);
    }

    public String getCellStringValue(int row, int column) {
        Cell value = sheet.getRow(row).getCell(column);
        return value == null ? "" : value.getStringCellValue();
    }

    public Double getCellNumericValue(int row, int column) {
        Cell value = sheet.getRow(row).getCell(column);
        return value == null ? 0 : value.getNumericCellValue();
    }

    @Override
    public int rowsCnt() {
        return data.size();
    }

    @Override
    public int columnsCnt() {
        return sheet.getRow(0).getPhysicalNumberOfCells();
    }

    @Override
    public String getCellString(ImportField field, int row, int column) throws ParseException {
        return getCellString(row, column);
    }


}
