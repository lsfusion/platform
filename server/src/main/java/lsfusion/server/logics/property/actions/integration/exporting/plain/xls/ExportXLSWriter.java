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
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportMatrixWriter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ExportXLSWriter extends ExportMatrixWriter {
    private boolean xlsx;
    private Workbook workbook;
    private Sheet sheet;
    private int rowNum = 0;

    public static class Styles {
        public final CellStyle date;
        public final CellStyle time;
        public final CellStyle dateTime;

        public Styles(Workbook workbook) {
            date = workbook.createCellStyle();
            date.setDataFormat(getDateFormat(workbook, DateClass.getDateFormat()));

            time = workbook.createCellStyle();
            time.setDataFormat(getDateFormat(workbook, TimeClass.getTimeFormat()));

            dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(getDateFormat(workbook, DateTimeClass.getDateTimeFormat()));
        }

        private short getDateFormat(Workbook workbook, DateFormat format) {
            assert format instanceof SimpleDateFormat;
            return workbook.createDataFormat().getFormat(((SimpleDateFormat) format).toPattern());
        }
    }
    private final Styles styles;    

    public ExportXLSWriter(ImOrderMap<String, Type> fieldTypes, boolean xlsx) throws IOException {
        super(fieldTypes, true);

        this.xlsx = xlsx;

        workbook = xlsx ? new XSSFWorkbook() : new HSSFWorkbook();
        sheet = workbook.createSheet();        
        styles = new Styles(workbook);
    }

    @Override
    protected void writeLine(ImMap<String, ?> values, ImMap<String, Type> types) {
        Row currentRow = sheet.createRow(rowNum++);
        for (int i=0,size=fieldIndexMap.size();i<size;i++) {
            Integer index = fieldIndexMap.getKey(i);
            String field = fieldIndexMap.getValue(i);

            fieldTypes.get(field).formatXLS(values.get(field), currentRow.createCell(index), styles);
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
}