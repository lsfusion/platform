package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.*;
import java.sql.Date;
import java.text.DateFormat;
import java.text.Format;
import java.util.*;

public class DateClass extends DataClass<Date> {

    public final static DateClass instance = new DateClass();

    public String toString() {
        return "Дата";
    }

    public int getPreferredWidth() { return 70; }

    public Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public Class getJavaClass() {
        return java.util.Date.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

    public byte getTypeID() {
        return Data.DATE;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof DateClass?this:null;
    }

    public Object getDefaultValue() {
        return 0;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getDateType();
    }

    public Date read(Object value) {
        if(value==null) return null;
        return (Date)value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setDate(num, (Date)value);
    }

    @Override
    public boolean isSafeType(Object value) {
        return false;
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        return (charBinary?1:2) * 25;
    }

    public boolean isSafeString(Object value) {
        return false;
    }
    
    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }
}
