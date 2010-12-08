package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public class LogicalClass extends DataClass<Boolean> {

    public static final LogicalClass instance = new LogicalClass();
    private final static String sid = "LogicalClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    public String toString() {
        return "Logical";
    }

    public int getPreferredWidth() { return 35; }

    public Format getDefaultFormat() {
        return null;
    }

    public Class getJavaClass() {
        return Boolean.class;
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        if (!super.fillReportDrawField(reportField))
            return false;

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_CENTER;
        return true;
    }

    public byte getTypeID() {
        return Data.LOGICAL;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof LogicalClass ?this:null;
    }

    public Object getDefaultValue() {
        return true;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getBitType();
    }

    public Boolean read(Object value) {
        if(value!=null) return true;
        return null;
    }

    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        assert (Boolean)value;
        return syntax.getBitString(true);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        assert (Boolean)value;
        statement.setByte(num, (byte)1);
    }

/*    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        return syntax.getBitString((Boolean) value);
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setByte(num, (byte) ((Boolean)value?1:0));
    }
  */

    @Override
    public int getBinaryLength(boolean charBinary) {
        return 1;
    }

    public Boolean shiftValue(Boolean object, boolean back) {
        return object==null?true:null;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Boolean.parseBoolean(s);
        } catch (Exception e) {
            throw new ParseException("error parsing boolean", e);
        }
    }

    public String getSID() {
        return sid;
    }
}
