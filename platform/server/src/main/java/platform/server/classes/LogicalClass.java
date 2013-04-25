package platform.server.classes;

import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public class LogicalClass extends DataClass<Boolean> {

    public static final LogicalClass instance = new LogicalClass();

    static {
        DataClass.storeClass(instance);
    }

    private LogicalClass() { super(ServerResourceBundle.getString("classes.logical"));}

    public String toString() {
        return "Logical";
    }

    public int getPreferredWidth() { return 50; }

    public Format getReportFormat() {
        return null;
    }

    public Class getReportJavaClass() {
        return Boolean.class;
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        if (!super.fillReportDrawField(reportField))
            return false;

        reportField.alignment = HorizontalAlignEnum.CENTER.getValue();
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
    public int getSQL(SQLSyntax syntax) {
        return syntax.getBitSQL();
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

    public Boolean parseString(String s) throws ParseException {
        try {
            return Boolean.parseBoolean(s);
        } catch (Exception e) {
            throw new ParseException("error parsing boolean", e);
        }
    }

    public String getSID() {
        return "LogicalClass";
    }

    @Override
    public Stat getTypeStat() {
        return Stat.ONE;
    }

    public boolean calculateStat() {
        return false;
    }
}
