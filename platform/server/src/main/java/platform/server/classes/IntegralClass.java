package platform.server.classes;

import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import platform.server.data.sql.SQLSyntax;
import platform.server.form.view.report.ReportDrawField;

import java.text.Format;
import java.text.NumberFormat;

// класс который можно сравнивать
public abstract class IntegralClass<T extends Number> extends DataClass<T> {

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 50; }

    protected IntegralClass(String caption) {
        super(caption);
    }

    public Format getReportFormat() {
        return NumberFormat.getInstance();
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        if (!super.fillReportDrawField(reportField))
            return false;

        reportField.alignment = HorizontalAlignEnum.RIGHT.getValue();
        return true;
    }
    
    @Override
    public Number getInfiniteValue() {
        throw new RuntimeException("not supported");
    }
    
    public Number getSafeInfiniteValue() { // бесконечное число которое можно сколько угодно суммировать и не выйти за тип
        return read(Math.round(Math.sqrt(getInfiniteValue().doubleValue())));
    }
    
    public Number div(Number obj, int div) {
        return read(obj.doubleValue() / 2);
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
