package lsfusion.server.logics.property.actions.importing.xls;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportXLSXWorkbookIterator extends ImportXLSXIterator {
    private XSSFWorkbook wb;
    private XSSFSheet currentSheet;
    private int currentSheetIndex;
    private int currentRow;
    private int lastRow;

    public ImportXLSXWorkbookIterator(byte[] file, List<Integer> columns, ImOrderSet<LCP> properties) throws IOException {
        super(columns, properties);

        wb = new XSSFWorkbook(new ByteArrayInputStream(file));
    }

    @Override
    public List<String> nextRow() {
        List<String> listRow = new ArrayList<>();
        try {
            if(currentSheet != null && currentRow >= lastRow) { //end of sheet
                currentSheet = null;
                currentSheetIndex++;
            }

            if(currentSheet == null && currentSheetIndex < wb.getNumberOfSheets()) { //end of workbook
                currentSheet = wb.getSheetAt(currentSheetIndex);
                currentRow = 0;
                lastRow = currentSheet.getLastRowNum();
                lastRow = lastRow == 0 && currentSheet.getRow(0) == null ? 0 : (lastRow + 1);
            }

            if(currentSheet != null) {
                XSSFRow xssfRow = currentSheet.getRow(currentRow);
                if (xssfRow != null) {
                    for (Integer column : columns) {
                        try {
                            listRow.add(getXLSXFieldValue(xssfRow, column, null));
                        } catch (Exception e) {
                            throw new RuntimeException(String.format("Error parsing row %s, column %s", currentRow, column + 1), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        currentRow++;
        return currentRow > lastRow && currentSheetIndex >= wb.getNumberOfSheets() ? null : listRow;
    }
}