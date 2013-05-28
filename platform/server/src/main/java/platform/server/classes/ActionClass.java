package platform.server.classes;

import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

// по умолчанию будем считать, что у ActionClass'а данные как у LogicalClass
public class ActionClass extends DataClass<Object> {

    public static final ActionClass instance = new ActionClass();

    public static final ObjectValue TRUE = ObjectValue.getValue(true, instance);

    private final static String sid = "ActionClass";
    static {
        DataClass.storeClass(instance);
    }

    private ActionClass() { super(ServerResourceBundle.getString("classes.action")); }

    @Override
    public String toString() {
        return "Action";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ActionClass ? this : null;
    }

    public Object getDefaultValue() {
        return true;
//        throw new RuntimeException("Неправильный вызов интерфейса");
    }

    @Override
    public byte getTypeID() {
        return Data.ACTION;
    }

    protected Class getReportJavaClass() {
        return Boolean.class;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getBitType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getBitSQL();
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        assert (Boolean)value;
        return syntax.getBitString(true);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        assert (Boolean)value;
        statement.setByte(num, (byte)1);
    }

    public Format getReportFormat() {
        return null;
    }

    public Object read(Object value) {
        if(value!=null) return true;
        return null;
    }

    @Override
    public boolean fillReportDrawField(ReportDrawField reportField) {
        return false;
    }

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    public String getSID() {
        return sid;
    }

    @Override
    public Stat getTypeStat() {
        return Stat.ONE;
    }
}
