package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DateClass extends DataClass<Integer> {

    public final static DateClass instance = new DateClass();

    public String toString() {
        return "Дата";
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        return new DataObject(randomizer.nextInt(30),this);
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        List<DataObject> result = new ArrayList<DataObject>();
        for(int i=0;i<30;i++)
            result.add(new DataObject(i,this));
        return result;
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
        return syntax.getIntegerType();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setInt(num, (Integer)value);
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }
}
