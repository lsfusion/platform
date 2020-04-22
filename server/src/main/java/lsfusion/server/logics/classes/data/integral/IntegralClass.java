package lsfusion.server.logics.classes.data.integral;

import lsfusion.base.BaseUtils;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.CustomDbfRecord;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.json.JSONException;
import org.json.JSONObject;

// класс который можно сравнивать
public abstract class IntegralClass<T extends Number> extends DataClass<T> {

    public int getReportMinimumWidth() { return 30; }
    public int getReportPreferredWidth() { return 50; }

    protected IntegralClass(LocalizedString caption) {
        super(caption);
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalTextAlignEnum.RIGHT;
    }

    @Override
    public boolean hasSafeCast() {
        return true;
    }

    @Override
    public T getInfiniteValue(boolean min) {
        throw new RuntimeException("not supported");
    }
    
    public Number getSafeInfiniteValue() { // бесконечное число которое можно сколько угодно суммировать и не выйти за тип
        return read(Math.round(Math.sqrt(getInfiniteValue(false).doubleValue())));
    }
    
    public Number div(Number obj, int div) {
        return read(obj.doubleValue() / 2);
    }

    public abstract int getWhole();
    public abstract int getPrecision();

    public IntegralClass getCompatible(DataClass compClass, boolean or) {
        if(!(compClass instanceof IntegralClass)) return null;

        IntegralClass integralClass = (IntegralClass)compClass;
        if(getWhole()>=integralClass.getWhole() && getPrecision()>=integralClass.getPrecision())
            return or ? this : integralClass;
        if(getWhole()<=integralClass.getWhole() && getPrecision()<=integralClass.getPrecision())
            return or ? integralClass : this;
        int whole = BaseUtils.cmp(getWhole(), integralClass.getWhole(), or);
        int precision = BaseUtils.cmp(getPrecision(), integralClass.getPrecision(), or);

        return NumericClass.get(whole+precision, precision);
    }

    public IntegralClass getMultiply(IntegralClass operator) {
//        if(1==1) return (IntegralClass) getCompatible(operator, true);

        if (this instanceof NumericClass || operator instanceof NumericClass) {
            int whole = getWhole() + operator.getWhole();
            int precision = getPrecision() + operator.getPrecision();

            return NumericClass.get(whole + precision, precision);
        } else {
            return getCompatible(operator, true);
        }
    }

    public IntegralClass getDivide(IntegralClass operator) {
//        if(1==1) return (IntegralClass) getCompatible(operator, true);

        if (this instanceof NumericClass || operator instanceof NumericClass) {
            int whole = getWhole() + operator.getPrecision();
            int precision = getPrecision() + operator.getWhole();
            if(Settings.get().isUseMaxDivisionLength())
                precision = Settings.get().getMaxNumericPrecision();

            return NumericClass.get(whole + precision, precision);
        } else {
            return getCompatible(operator, true);
        }
    }

    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        if (isNegative(read(value)))
            return "(" + value.toString() + ")";
        else
            return value.toString();
    }
    protected abstract boolean isNegative(T value);
    public abstract boolean isPositive(T obj);

    @Override
    public boolean isValueZero(T value) {
        double doubleValue = value.doubleValue();
        return doubleValue > -0.0005 && doubleValue < 0.0005;
    }

    @Override
    public boolean fixedSize() { // так как NumericClass не fixedSize, а getCompatible может "смешивать" другие Integral с NumericeClass'ами
        return false;
    }

    @Override
    public String formatString(T value) {
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public T parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws ParseException {
        return readDBF(dbfRecord.getNumber(fieldName));
    }

    @Override
    public T parseJSON(Object value) throws JSONException {
        if(value == JSONObject.NULL || (value instanceof String && (value.equals("NaN") || value.equals("Infinity") || value.equals("-NaN") || value.equals("-Infinity"))))
            return null;
        return readJSON((Number) value);
    }

    @Override
    public T parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        if(formulaValue.getCellTypeEnum().equals(CellType.NUMERIC))
            return readXLS(formulaValue.getNumberValue());
        return super.parseXLS(cell, formulaValue); 
    }

    @Override
    public Object formatJSON(T object) {
        return object;
    }

    @Override
    public String getJSONType() {
        return "number";
    }

    @Override
    public void formatXLS(T object, Cell cell, ExportXLSWriter.Styles styles) {
        if(object != null)
            cell.setCellValue(object.doubleValue());
    }

    @Override
    public boolean isFlex() {
        return true;
    }

    @Override
    public FlexAlignment getValueAlignment() {
        return FlexAlignment.END;
    }
}
