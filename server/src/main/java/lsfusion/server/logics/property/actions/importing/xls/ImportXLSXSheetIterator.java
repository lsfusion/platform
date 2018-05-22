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

public class ImportXLSXSheetIterator extends ImportXLSXIterator {
    private int current;
    private XSSFSheet sheet;
    private int lastRow;

    public ImportXLSXSheetIterator(byte[] file, List<Integer> columns, ImOrderSet<LCP> properties, Integer sheetIndex) throws IOException {
        super(columns, properties);

        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file));
        sheet = wb.getSheetAt(sheetIndex == null ? 0 : (sheetIndex - 1));
        lastRow = sheet.getLastRowNum();
        lastRow = lastRow == 0 && sheet.getRow(0) == null ? 0 : (lastRow + 1);
    }

    @Override
    public List<String> nextRow() {
        List<String> listRow = new ArrayList<>();
        try {
            XSSFRow xssfRow = sheet.getRow(current);
            if (xssfRow != null) {
                for (Integer column : columns) {
                    try {
                        listRow.add(getXLSXFieldValue(xssfRow, column, null));
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Error parsing row %s, column %s", current, column+1), e);
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        current++;
        return current > lastRow ? null : listRow;
    }
}