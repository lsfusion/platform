package platform.server.data.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.session.SQLSession;
import platform.server.view.form.client.report.ReportDrawField;
import platform.interop.Data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.*;

public class LogicalClass extends DataClass<Boolean> {

    public static final LogicalClass instance = new LogicalClass();

    public String toString() {
        return "Logical";
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        return new DataObject(true,this);
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        return Collections.singletonList(new DataObject(true,this));
    }

    public int getPreferredWidth() { return 35; }

    public Format getDefaultFormat() {
        return null;
    }

    public Class getJavaClass() {
        return Boolean.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_CENTER;
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

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
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
}
