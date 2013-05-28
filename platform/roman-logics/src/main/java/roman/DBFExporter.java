package roman;

import org.xBaseJ.DBF;
import org.xBaseJ.fields.*;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.form.instance.FormData;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.DataSession;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Map;

public class DBFExporter {
    DataSession session;
    ImMap<ClassPropertyInterface, DataObject> keys;
    FormData data;
    Map<Field, PropertyDrawInstance> map;

    public DBFExporter(ImMap<ClassPropertyInterface, DataObject> keys) {
        this.keys = keys;
    }

    public void putString(CharField field, Object value) throws xBaseJException {
        field.put(BaseUtils.padr(value != null ? (String) value : "", field.getLength()));
    }

    public void putDouble(NumField field, Object value) throws xBaseJException {
        if (value != null) {
            field.put(valueOfField(field, (Double) value).getBytes());
        }
    }

    public void putDate(DateField field, Object value) throws xBaseJException {
        if (value != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(DateConverter.sqlToDate((java.sql.Date) value));
            field.put(calendar);
        } else {
            field.put(BaseUtils.padr("", field.getLength()));
        }
    }

    public void putLogical(LogicalField field, Object value) throws xBaseJException {
        if (value != null) {
            field.put((Boolean) value);
        }
    }

    // метод нужен лишь потому, что xBaseJ не умеет нормально писать в dbf-файлы Double
    public String valueOfField(NumField fld, Double val) {

        int intlen = fld.getLength() - fld.getDecimalPositionCount() - 1;
        int declen = fld.getDecimalPositionCount();

        String pattern = "";
        for (int i = 0; i < intlen - 1; i++) pattern += "#";
        pattern += "0.";
        for (int i = 0; i < declen; i++) pattern += "0";

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        return format.format(val);
    }

    public class CustomDBF extends DBF {
        public CustomDBF(String DBFName, int format, boolean destroy, String inEncodeType) throws IOException, xBaseJException {
            super(DBFName, format, destroy, inEncodeType);
        }

        public CustomDBF(String DBFName, boolean destroy, String inEncodeType) throws IOException, xBaseJException {
            super(DBFName, destroy, inEncodeType);
        }

        public File getFFile() {
            return ffile;
        }
    }
}
