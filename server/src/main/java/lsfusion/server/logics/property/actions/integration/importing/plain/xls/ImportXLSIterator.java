package lsfusion.server.logics.property.actions.integration.importing.plain.xls;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainIterator;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;

public class ImportXLSIterator extends ImportPlainIterator {

    private final Workbook wb; 
    private FormulaEvaluator formulaEvaluator;
    private int rowsCount;
    private Integer lastSheet;
    
    public ImportXLSIterator(ImOrderMap<String, Type> fieldTypes, byte[] file, boolean xlsx, Integer singleSheetIndex) throws IOException {
        super(fieldTypes);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(file);
        if(xlsx) {
            wb = new XSSFWorkbook(inputStream);
            formulaEvaluator = new XSSFFormulaEvaluator((XSSFWorkbook) wb);
        } else {
            wb = new HSSFWorkbook(inputStream);
            formulaEvaluator = new HSSFFormulaEvaluator((HSSFWorkbook) wb);
        }
        if(singleSheetIndex != null) {
            currentSheet = singleSheetIndex;
            lastSheet = singleSheetIndex;
        } else {
            currentSheet = 0;
            lastSheet = wb.getNumberOfSheets();
        }            
        
        finalizeInit();
    }

    private ImMap<String, Integer> fieldIndexes;

    @Override
    protected ImOrderSet<String> readFields() {
        ImOrderMap<String, Integer> sourceMap = nameToIndexColumnsMapping;
        fieldIndexes = sourceMap.getMap();
        return sourceMap.keyOrderSet();
    }

    private int currentSheet = 0;
    private Sheet sheet = null;
    private boolean nextSheet() {
        if(currentSheet > lastSheet)
            return false;
        
        sheet = wb.getSheetAt(currentSheet++);
        rowsCount = sheet.getLastRowNum() + 1;
        return true;
    }
    
    private int currentRow = 0;
    private Row row;
    @Override
    protected boolean nextRow() throws IOException {
        if (sheet == null || currentRow >= rowsCount) {
            if(!nextSheet())
                return false;
            currentRow = 0;
        }
        
        row = sheet.getRow(currentRow++);
        if(row == null)
            return nextRow();
        return true;
    }

    @Override
    protected Object getPropValue(String name, Type type) throws lsfusion.server.data.type.ParseException, ParseException {
        Cell cell = row.getCell(fieldIndexes.get(name));
        if(cell == null)
            return null;
        CellValue cellValue = formulaEvaluator.evaluate(cell);
        if(cellValue == null)
            return null;
        try {
            return type.parseXLS(cell, cellValue);
        } catch (lsfusion.server.data.type.ParseException e) {
            return null; // xls is really unpredictable format, so will return null if something is going wrong
        }
    }

    @Override
    public void release() throws IOException {
        if(wb != null)
            wb.close();
    }

    public final static ImOrderMap<String, Integer> nameToIndexColumnsMapping = ListFact.consecutiveList(256, 0).mapOrderKeys(new GetValue<String, Integer>() {
        public String getMapValue(Integer value) {
            return nameToIndex(value);
        }
    });

    private static String nameToIndex(int index) {
        String columnName = "";
        int resultLen = 1;
        final int LETTERS_CNT = 26;
        int sameLenCnt = LETTERS_CNT;
        while (sameLenCnt <= index) {
            ++resultLen;
            index -= sameLenCnt;
            sameLenCnt *= LETTERS_CNT;
        }

        for (int i = 0; i < resultLen; ++i) {
            columnName = (char)('A' + (index % LETTERS_CNT)) + columnName;
            index /= LETTERS_CNT;
        }
        return columnName;
    }
}
