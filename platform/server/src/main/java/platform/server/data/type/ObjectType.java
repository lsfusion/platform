package platform.server.data.type;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;

public class ObjectType implements Type<Integer> {

    public ObjectType() {
        super();
    }

    public boolean isCompatible(Type type) {
        return type instanceof ObjectType;
    }

    public static final ObjectType instance = new ObjectType();

    public String getDB(SQLSyntax syntax) {
        return syntax.getIntegerType();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setInt(num, (Integer)value);
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

    public DataObject getEmptyValueExpr() {
        throw new RuntimeException("temporary");
    }

    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }
    public int getMinimumWidth() { return getPreferredWidth(); }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = Integer.class;
        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

}
