package lsfusion.server.logics.form.stat.struct.imports.plain.xls;

import com.monitorjbl.xlsx.StreamingReader;
import com.monitorjbl.xlsx.exceptions.NotSupportedException;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportMatrixIterator;
import lsfusion.server.physics.admin.Settings;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class ImportXLSIterator extends ImportMatrixIterator {

    private final Workbook wb;
    private File wbFile;
    private FormulaEvaluator formulaEvaluator;
    private Integer lastSheet;
    
    public ImportXLSIterator(ImOrderMap<String, Type> fieldTypes, RawFileData file, boolean xlsx, String wheres, boolean noHeader, Integer singleSheetIndex) throws IOException {
        super(fieldTypes, wheres, noHeader);

        int minSize = Settings.get().getMinSizeForExcelStreamingReader();
        useStreamingReader = xlsx && minSize >= 0 && file.getLength() >= minSize;
        if(useStreamingReader) {
            wbFile = File.createTempFile("import", "xlsx");
            file.write(wbFile);
            wb = StreamingReader.builder().rowCacheSize(100)    // number of rows to keep in memory (defaults to 10)
                    .bufferSize(4096)     // buffer size to use when reading InputStream to file (defaults to 1024)
                    .open(wbFile);            // InputStream or File for XLSX file (required)
            formulaEvaluator = new XLSXHugeFormulaEvaluator();
        } else {
            InputStream inputStream = file.getInputStream();
            if (xlsx) {
                wb = new XSSFWorkbook(inputStream);
                formulaEvaluator = new XSSFFormulaEvaluator((XSSFWorkbook) wb);
            } else {
                wb = new HSSFWorkbook(inputStream);
                formulaEvaluator = new HSSFFormulaEvaluator((HSSFWorkbook) wb);
            }
        }

        if (singleSheetIndex != null) {
            currentSheet = singleSheetIndex;
            lastSheet = singleSheetIndex;
        } else {
            currentSheet = 0;
            lastSheet = wb.getNumberOfSheets() - 1; //zero based
        }            
        
        finalizeInit();
    }

    private int currentSheet = 0;
    private Sheet sheet = null;

    private boolean nextSheet() {
        if (currentSheet > lastSheet) {
            return false;
        }
        sheet = wb.getSheetAt(currentSheet++);
        return true;
    }

    private Iterator<Row> rowIterator = null;
    private Row row;

    private final boolean useStreamingReader;
    private boolean firstRow;

    @Override
    protected boolean nextRow(boolean checkWhere) {
        while(true) {
            if (sheet == null || !rowIterator.hasNext()) {
                if (!nextSheet()) return false;
                rowIterator = sheet.rowIterator();

                if (useStreamingReader) firstRow = true;

                return nextRow(checkWhere);
            }
            row = rowIterator.next();

            if (checkWhere && ignoreRow()) {
                continue;
            }

            if (useStreamingReader && row.getRowNum() == 0) {
                if (firstRow) { //skip all rows with rowNum 0 (except first) - they are incorrect
                    firstRow = false;
                } else {
                    return nextRow(checkWhere);
                }
            }

            return true;
        }
    }

    @Override
    protected Integer getRowIndex() {
        return row.getRowNum() - (noHeader ? 0 : 1);
    }

    @Override
    protected Object getPropValue(Integer fieldIndex, Type type) throws ParseException {
        Cell cell = row.getCell(fieldIndex);
        if(cell == null)
            return null;

        //hack: some files has formulas starting with "=" - apache.poi can not parse it, so we remove "="
        if (cell.getCellType() == CellType.FORMULA) {
            String formula = cell.getCellFormula();
            if (formula != null && formula.startsWith("=")) {
                cell.setCellFormula(formula.substring(1));
            }
        }

        CellValue cellValue = formulaEvaluator.evaluate(cell);
        if(cellValue == null)
            return null;
        try {
            return type.parseXLS(cell, cellValue);
        } catch (ParseException e) {
            throw ParseException.propagateWithMessage(String.format(" (row %s, column %s)", cell.getRowIndex() + 1, cell.getColumnIndex() + 1), e);
        }
    }

    @Override
    protected boolean isLastValue(Integer fieldIndex) {
        return fieldIndex >= row.getLastCellNum();
    }

    @Override
    public void release() throws IOException {
        if(wb != null)
            wb.close();
        if(useStreamingReader) {
            if (wbFile != null && !wbFile.delete()) {
                wbFile.deleteOnExit();
            }
        }
    }

    private class XLSXHugeFormulaEvaluator implements FormulaEvaluator {
        @Override
        public void clearAllCachedResultValues() {
        }

        @Override
        public void notifySetFormula(Cell cell) {
        }

        @Override
        public void notifyDeleteCell(Cell cell) {
        }

        @Override
        public void notifyUpdateCell(Cell cell) {
        }

        @Override
        public void evaluateAll() {
        }

        //from BaseFormulaEvaluator
        @Override
        public CellValue evaluate(Cell cell) {
            if (cell == null) {
                return null;
            }
            switch (cell.getCellType()) {
                case BOOLEAN:
                    return CellValue.valueOf(cell.getBooleanCellValue());
                case ERROR:
                    try {
                        return CellValue.getError(cell.getErrorCellValue());
                    } catch (NotSupportedException e) {
                        return null;
                    }
                case FORMULA:
                    switch (cell.getCachedFormulaResultType()) {
                        case NUMERIC:
                            return new CellValue(cell.getNumericCellValue());
                        default:
                            return new CellValue(cell.getRichStringCellValue().getString());
                    }
                case NUMERIC:
                    return new CellValue(cell.getNumericCellValue());
                case STRING:
                    return new CellValue(cell.getRichStringCellValue().getString());
                case BLANK:
                    return null;
                default:
                    throw new IllegalStateException("Bad cell type (" + cell.getCellType() + ")");
            }
        }

        @Override
        public CellType evaluateFormulaCell(Cell cell) {
            return CellType.NUMERIC;
        }

        @Override
        public CellType evaluateFormulaCellEnum(Cell cell) {
            return null;
        }

        @Override
        public Cell evaluateInCell(Cell cell) {
            return null;
        }

        @Override
        public void setupReferencedWorkbooks(Map<String, FormulaEvaluator> workbooks) {
        }

        @Override
        public void setIgnoreMissingWorkbooks(boolean ignore) {
        }

        @Override
        public void setDebugEvaluationOutputForNextEval(boolean value) {
        }
    }
}
