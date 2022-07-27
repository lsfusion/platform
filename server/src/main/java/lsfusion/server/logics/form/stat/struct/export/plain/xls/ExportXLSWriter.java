package lsfusion.server.logics.form.stat.struct.export.plain.xls;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportMatrixWriter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ExportXLSWriter extends ExportMatrixWriter {
    private Workbook workbook;
    private Sheet sheet;
    private int rowNum = 0;

    public static class Styles {
        public final CellStyle date;
        public final CellStyle time;
        public final CellStyle dateTime;

        public Styles(Workbook workbook) {
            date = workbook.createCellStyle();
            date.setDataFormat(getDateFormat(workbook, DateFormat.getDateInstance(DateFormat.SHORT)));

            time = workbook.createCellStyle();
            time.setDataFormat(getDateFormat(workbook, DateFormat.getTimeInstance(DateFormat.MEDIUM)));

            dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(getDateFormat(workbook, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)));
        }

        private short getDateFormat(Workbook workbook, DateFormat format) {
            assert format instanceof SimpleDateFormat;
            return workbook.createDataFormat().getFormat(((SimpleDateFormat) format).toPattern());
        }
    }
    private final Styles styles;    

    public ExportXLSWriter(ImOrderMap<String, Type> fieldTypes, boolean xlsx, boolean noHeader, String sheetName) throws IOException {
        super(fieldTypes, noHeader);

        workbook = xlsx ? new XSSFWorkbook() : new HSSFWorkbook();
        sheet = sheetName != null ? workbook.createSheet(sheetName) : workbook.createSheet();
        styles = new Styles(workbook);

        finalizeInit();
    }

    @Override
    protected void writeLine(ImMap<String, ?> values, ImMap<String, Type> types) {
        Row currentRow = sheet.createRow(rowNum++);
        for (int i=0,size=fieldIndexMap.size();i<size;i++) {
            Integer index = fieldIndexMap.getKey(i);
            String field = fieldIndexMap.getValue(i);

            types.get(field).formatXLS(values.get(field), currentRow.createCell(index), styles);
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