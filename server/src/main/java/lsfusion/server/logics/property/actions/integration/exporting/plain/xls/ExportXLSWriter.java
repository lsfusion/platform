package lsfusion.server.logics.property.actions.integration.exporting.plain.xls;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.TimeClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportByteArrayPlainWriter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ExportXLSWriter extends ExportByteArrayPlainWriter {
    private boolean xlsx;
    private Workbook workbook;
    private Sheet sheet;

    CellStyle dateStyle;
    CellStyle timeStyle;
    CellStyle dateTimeStyle;

    public ExportXLSWriter(ImOrderMap<String, Type> fieldTypes, boolean xlsx) throws IOException {
        super(fieldTypes);

        this.xlsx = xlsx;

        workbook = xlsx ? new XSSFWorkbook() : new HSSFWorkbook();
        sheet = workbook.createSheet();

        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(getDateFormat(DateClass.getDateFormat()));

        timeStyle = workbook.createCellStyle();
        timeStyle.setDataFormat(getDateFormat(TimeClass.getTimeFormat()));

        dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(getDateFormat(DateTimeClass.getDateTimeFormat()));
    }


    @Override
    public void writeLine(int rowNum, final ImMap<String, Object> row) {

        ImOrderMap<String, Object> rowValues = fieldTypes.mapOrderValues(new GetKeyValue<Object, String, Type>() {
            @Override
            public Object getMapValue(String key, Type value) {
                return value.formatXLS(row.get(key));
            }
        });

        Row currentRow = sheet.createRow(rowNum);
        Integer prevIndex = 0;
        for (String field : fieldTypes.keyOrderSet()) {

            Integer index = nameToIndex(field, xlsx);
            if(index == null) {
                index = ++prevIndex;
            } else {
                prevIndex = index;
            }
            Cell cell = currentRow.createCell(index);
            Object value = rowValues.get(field);
            if (value instanceof Date) {
                cell.setCellValue(((Date) value));
                cell.setCellStyle(dateStyle);
            } else if (value instanceof Time) {
                cell.setCellValue(((Time) value));
                cell.setCellStyle(timeStyle);
            } else if (value instanceof Timestamp) {
                cell.setCellValue(((Timestamp) value));
                cell.setCellStyle(dateTimeStyle);
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else if(value instanceof Double) {
                cell.setCellValue((Double) value);
            } else
                cell.setCellValue(value == null ? null : String.valueOf(value));
        }
    }

    protected void closeWriter() {
        try {
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static Integer nameToIndex(String name, boolean xlsx) {
        Integer number = null;
        //restrictions: max 256 (xls) or 16384 (xlsx) columns and uppercase
        if(name.equals(name.toUpperCase())) {
            for (int i = 0; i < name.length(); i++) {
                number = (number== null ? 0 : number * 26) + (name.charAt(i) - ('A' - 1));
            }
        }
        return number != null && number <= (xlsx ? 16384 : 256) ? (number - 1) : null;
    }

    private short getDateFormat(DateFormat format) {
        assert format instanceof SimpleDateFormat;
        return workbook.createDataFormat().getFormat(((SimpleDateFormat) format).toPattern());
    }
}