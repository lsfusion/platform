package roman;

import jxl.*;
import jxl.read.biff.BiffException;
import platform.base.BaseUtils;
import platform.server.classes.DateClass;
import platform.server.classes.DoubleClass;
import platform.server.integration.ExcelInputTable;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import static platform.server.integration.SingleSheetImporter.*;

public class DieselInputTable implements ImportInputTable {
    jxl.Sheet sheet;
    private List<List<String>> data = new ArrayList<List<String>>();

    public DieselInputTable(InputStream stream) throws BiffException, IOException {

        Workbook Wb = Workbook.getWorkbook(stream);
        sheet = Wb.getSheet(0);
        List<String> row;

        for (int i = 2; i < sheet.getRows(); i++) {    //2
            row = new ArrayList<String>();
            row.add(getCellString(i, A));  //invoiceSIDField
            row.add(getCellString(i, B));  //dateInvoiceField
            row.add(getCellString(i, C)); //numberSKUField
            row.add(getCellString(i, Q));  //countryField
            row.add(getCellString(i, R));  //originalNameField
            row.add(getCellString(i, U));  //colorCodeField
            row.add(getCellString(i, Z));  //customCodeField
            row.add(getCellString(i, Z));  //customCode6Field
            row.add(getCellString(i, AA)); //compositionField
            row.add(getCellString(i, AC)); //unitPriceField
            row.add(getCellString(i, AC)); //RRPField
            row.add(getCellString(i, AF)); //boxNumberField
            row.add(getCellString(i, S)); //sidField
            row.add(""); //colorNameField
            row.add(""); //unitNetWeightField

            for (int j = AI; j <= AX; j++) {
                List<String> tempRow = new ArrayList<String>();
                for (String str : row)
                    tempRow.add(str);
                if (getCellString(i, j) != "") {
                    if (Integer.parseInt(getCellString(i, AH)) != 25)
                        tempRow.add(getCellString(4, j)); //sizeField //0
                    else
                        tempRow.add(getCellString(5, j));    //1
                    tempRow.add(getCellString(i, j)); //unitQuantityField
                    data.add(tempRow);
                }
            }
        }
    }

    @Override
    public String getCellString(int row, int column) {
        return sheet.getCell(column, row).getContents();
    }

    public String getCellVal(int row, int column) {
        return data.get(row).get(column);
    }

    public String getCellVal(ImportField field, int row, int column) {
        return data.get(row).get(column);
    }

    @Override
    public int rowsCnt() {
        return data.size();
    }

    @Override
    public int columnsCnt() {
        return sheet.getColumns();
    }

    @Override
    public String getCellString(ImportField field, int row, int column) throws ParseException {
        return getCellString(row, column);
    }


}
