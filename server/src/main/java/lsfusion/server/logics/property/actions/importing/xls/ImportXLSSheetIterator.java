package lsfusion.server.logics.property.actions.importing.xls;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.IncorrectFileException;
import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportXLSSheetIterator extends ImportXLSIterator {
    private int current;
    private HSSFSheet sheet;
    private int lastRow;

    public ImportXLSSheetIterator(byte[] file, List<Integer> columns, ImOrderSet<LCP> properties, Integer sheetIndex) throws IOException, IncorrectFileException {
        super(columns, properties);

        try {
            HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(file));

            sheet = wb.getSheetAt(sheetIndex == null ? 0 : (sheetIndex - 1));
            lastRow = sheet.getLastRowNum();
            lastRow = lastRow == 0 && sheet.getRow(0) == null ? 0 : (lastRow + 1);
        } catch (OldExcelFormatException e) {
            throw new IncorrectFileException(e.getMessage());
        }
    }

    @Override
    public List<String> nextRow() {
        List<String> listRow = new ArrayList<>();
        try {
            HSSFRow hssfRow = sheet.getRow(current);
            if (hssfRow != null) {
                for (Integer column : columns) {
                    try {
                        listRow.add(getXLSFieldValue(hssfRow, column, null));
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