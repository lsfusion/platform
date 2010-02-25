package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

// класс который можно сравнивать
public abstract class IntegralClass<T extends Number> extends DataClass<T> {

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        return new DataObject(50,this);
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        List<DataObject> result = new ArrayList<DataObject>();
        for(int i=0;i<50;i++)
            result.add(new DataObject(i,this));
        return result;
    }


    public int getMinimumWidth() { return 45; }
    public int getPreferredWidth() { return 80; }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

    public Object getDefaultValue() {
        return 0;
    }

    abstract int getWhole();
    abstract int getPrecision();

    public DataClass getCompatible(DataClass compClass) {
        if(!(compClass instanceof IntegralClass)) return null;

        IntegralClass integralClass = (IntegralClass)compClass;
        if(getWhole()>=integralClass.getWhole() && getPrecision()>=integralClass.getPrecision())
            return this;
        if(getWhole()<=integralClass.getWhole() && getPrecision()<=integralClass.getPrecision())
            return integralClass;
        if(getWhole()>integralClass.getWhole())
            return NumericClass.get(getWhole()+integralClass.getPrecision(), integralClass.getPrecision());
        else
            return NumericClass.get(integralClass.getWhole()+getPrecision(), getPrecision());
    }

    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }
}
