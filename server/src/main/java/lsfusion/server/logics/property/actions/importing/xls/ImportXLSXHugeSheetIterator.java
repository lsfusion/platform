package lsfusion.server.logics.property.actions.importing.xls;

import com.google.common.base.Throwables;
import com.monitorjbl.xlsx.StreamingReader;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ImportXLSXHugeSheetIterator extends ImportXLSXIterator {
    private Iterator<List<Cell>> rowIterator;

    public ImportXLSXHugeSheetIterator(byte[] file, List<Integer> columns, ImOrderSet<LCP> properties, Integer sheetIndex) throws IOException {
        super(columns, properties);

        File tmpFile = File.createTempFile("import", "xlsx");
        try {
            FileUtils.writeByteArrayToFile(tmpFile, file);
            try(
            InputStream is = new FileInputStream(tmpFile);
            Workbook workbook = StreamingReader.builder()
                    .rowCacheSize(100)    // number of rows to keep in memory (defaults to 10)
                    .bufferSize(4096)     // buffer size to use when reading InputStream to file (defaults to 1024)
                    .open(is)            // InputStream or File for XLSX file (required)
            ) {
                Sheet sheet = workbook.getSheetAt(sheetIndex == null ? 0 : (sheetIndex - 1));
                boolean first = true;
                List<List<Cell>> data = new ArrayList<>();
                for (Row r : sheet) {
                    if(r.getRowNum() != 0 || first) {
                        first = false;
                        List<Cell> cellList = new ArrayList<>();
                        for (Integer column : columns) {
                            Cell cell = r.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            //System.out.println("Reading row/cell " + r.getRowNum() + "/" + cell.getColumnIndex());
                            cellList.add(cell);
                        }
                        if (!cellList.isEmpty()) {
                            data.add(cellList);
                        }
                    }
                }
                rowIterator = data.iterator();
            }


        } catch (Throwable e) {
            throw Throwables.propagate(e);
        } finally {
            if(!tmpFile.delete())
                tmpFile.deleteOnExit();
        }
    }

    @Override
    public List<String> nextRow() {
        if(rowIterator.hasNext()) {
            List<String> listRow = new ArrayList<>();
            try {
                List<Cell> xssfRow = rowIterator.next();
                if (xssfRow != null) {
                    for(int c = 0; c < xssfRow.size(); c++) {
                        Cell cell = xssfRow.get(c);
                            try {
                                listRow.add(getXLSXFieldValue(cell));
                            } catch (Exception e) {
                                throw new RuntimeException(String.format("Error parsing row %s, column %s", cell.getRowIndex(), cell.getColumnIndex() + 1), e);
                            }
                    }
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            return listRow;
        } else {
            return null;
        }
    }

    private String getXLSXFieldValue(Cell cell) {
        String result = null;
        if (cell != null) {
            DateFormat dateFormat = getDateFormat(properties, columns, cell.getColumnIndex());
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_NUMERIC:
                    if (dateFormat != null) {
                        Date value = getDateValue(cell);
                        if (value != null) result = dateFormat.format(value);
                    } else {
                        result = new DecimalFormat("#.#####").format(cell.getNumericCellValue());
                        result = result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
                    }
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    try {
                        if (dateFormat != null) {
                            String formulaCellValue = getFormulaCellValue(cell);
                            result = formulaCellValue.isEmpty() ? null : formulaCellValue.trim();
                            if (result != null) result = parseFormatDate(dateFormat, result);
                        } else {
                            result = new DecimalFormat("#.#####").format(cell.getNumericCellValue());
                        }
                    } catch (Exception e) {
                        String formulaCellValue = getFormulaCellValue(cell);
                        result = formulaCellValue.isEmpty() ? null : formulaCellValue;
                    }
                    result = result != null && result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
                    break;
                case Cell.CELL_TYPE_ERROR:
                    result = null;
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    result = String.valueOf(cell.getBooleanCellValue());
                    break;
                case Cell.CELL_TYPE_STRING:
                default:
                    if (dateFormat != null) {
                        result = (cell.getStringCellValue().isEmpty()) ? null : cell.getStringCellValue().trim();
                        if (result != null) result = parseFormatDate(dateFormat, result);
                    } else {
                        result = (cell.getStringCellValue().isEmpty()) ? null : cell.getStringCellValue().trim();
                    }
            }
        }
        return result;
    }
}