package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BitClass extends DataClass<Boolean> {

    public static final BitClass instance = new BitClass();

    public String toString() {
        return "Bit";
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        return new DataObject(randomizer.nextBoolean(),this);
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        List<DataObject> result = new ArrayList<DataObject>();
        result.add(new DataObject(false,this));
        result.add(new DataObject(true,this));
        return result;
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
        return Data.BIT;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof BitClass ?this:null;
    }

    public Object getDefaultValue() {
        return true;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getBitType();
    }

    public Boolean read(Object value) {
        if(value instanceof Number)
            return ((Number)value).byteValue()!=0;
        else
            return (Boolean) value;
    }

    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        return syntax.getBitString((Boolean) value);
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setByte(num, (byte) ((Boolean)value?1:0));
    }

    public int getBinaryLength() {
        return 1;
    }
}
