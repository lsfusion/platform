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

public class DieselInvoiceInputTable implements ImportInputTable {
    jxl.Sheet sheet;
    private List<List<String>> data = new ArrayList<List<String>>();

    public DieselInvoiceInputTable(InputStream stream) throws BiffException, IOException {

        Workbook Wb = Workbook.getWorkbook(stream);
        sheet = Wb.getSheet(0);
        List<String> row;

        for (int i = 2; i < sheet.getRows(); i++) {
            row = new ArrayList<String>();
            row.add(sheet.getCell(A, i).getContents());  //invoiceSIDField
            row.add(sheet.getCell(B, i).getContents());  //dateInvoiceField
            row.add(sheet.getCell(C, i).getContents());  //numberSKUField
            row.add(sheet.getCell(D, i).getContents());  //themeCodeField
            row.add(sheet.getCell(D, i).getContents());  //themeNameField
            row.add(sheet.getCell(Q, i).getContents());  //countryField
            row.add(sheet.getCell(R, i).getContents());  //originalNameField
            row.add(sheet.getCell(U, i).getContents());  //colorCodeField
            row.add(sheet.getCell(W, i).getContents());  //genderField
            row.add(sheet.getCell(Z, i).getContents());  //customCodeField
            row.add(sheet.getCell(Z, i).getContents());  //customCode6Field
            row.add(sheet.getCell(AA, i).getContents()); //compositionField
            row.add(sheet.getCell(AC, i).getContents()); //unitPriceField
            row.add(sheet.getCell(AC, i).getContents()); //RRPField
            row.add(sheet.getCell(AF, i).getContents()); //boxNumberField
            row.add(sheet.getCell(S, i).getContents()); //sidField
            row.add(""); //colorNameField
            row.add(""); //unitNetWeightField
            row.add(""); //subCategoryCodeField
            row.add(""); //collectionCodeField
            row.add(""); //subCategoryNameField
            row.add(""); //collectionNameField

            for (int j = AI; j <= AX; j++) {
                List<String> tempRow = new ArrayList<String>();
                for (String str : row)
                    tempRow.add(str);
                if (sheet.getCell(j, i).getContents() != "") {
                    if (Integer.parseInt(sheet.getCell(AH, i).getContents()) != 25)
                        tempRow.add(sheet.getCell(j, 4).getContents()); //sizeField
                    else
                        tempRow.add(sheet.getCell(j, 5).getContents());
                    tempRow.add(sheet.getCell(j, i).getContents()); //unitQuantityField
                    data.add(tempRow);
                }
            }
        }
    }

    @Override
    public String getCellString(int row, int column) {
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
