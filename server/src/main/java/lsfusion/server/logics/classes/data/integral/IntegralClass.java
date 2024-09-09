package lsfusion.server.logics.classes.data.integral;

import lsfusion.base.BaseUtils;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TextBasedClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
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
public abstract class IntegralClass<T extends Number> extends TextBasedClass<T> implements DBType {

    public int getReportMinimumWidth() { return 30; }
    public int getReportPreferredWidth() { return 50; }

    protected IntegralClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalTextAlignEnum.RIGHT;
    }

    @Override
    public DBType getDBType() {
        return this;
    }

    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        boolean isInt = castType.isArith() || typeFrom instanceof IntegralClass || typeFrom instanceof ObjectType;
        boolean isStr = typeFrom instanceof StringClass;
        if(!(isInt && Settings.get().getSafeCastIntType() == 2)) {
            if(typeFrom != null && equalsDB(typeFrom))
                return value;

            int sourceType = isInt ? 0 : isStr ? 1 : 2;
            typeEnv.addNeedSafeCast(this, sourceType);
            return syntax.getSafeCastNameFnc(this, sourceType) + "(" + value + ")";
        }

        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    @Override
    public boolean isCastNotNull(Type typeFrom, CastType castType) {
        if(typeFrom == null) // we don't know the type it can be any type
            return true;

        if(equalsDB(typeFrom))
            return false;

        boolean isInt = castType.isArith() || typeFrom instanceof IntegralClass || typeFrom instanceof ObjectType;
        if(!(isInt && Settings.get().getSafeCastIntType() == 2))
            return true;

        return super.isCastNotNull(typeFrom, castType);
    }

    @Override
    public T getInfiniteValue(boolean min) {
        throw new RuntimeException("not supported");
    }
    
    public Number getSafeInfiniteValue() { // бесконечное число которое можно сколько угодно суммировать и не выйти за тип
        return readNumber(Math.round(Math.sqrt(getInfiniteValue(false).doubleValue())));
    }
    
    public Number div(Number obj, int div) {
        return readNumber(obj.doubleValue() / 2);
    }

    public abstract int getWhole();
    public abstract int getScale();

    public IntegralClass getCompatible(DataClass compClass, boolean or) {
        if(!(compClass instanceof IntegralClass)) return null;

        IntegralClass integralClass = (IntegralClass)compClass;
        if(getWhole()>=integralClass.getWhole() && getScale()>=integralClass.getScale())
            return or ? this : integralClass;
        if(getWhole()<=integralClass.getWhole() && getScale()<=integralClass.getScale())
            return or ? integralClass : this;
        int whole = BaseUtils.cmp(getWhole(), integralClass.getWhole(), or);
        int scale = BaseUtils.cmp(getScale(), integralClass.getScale(), or);

        return NumericClass.get(whole+scale, scale);
    }

    public IntegralClass getMultiply(IntegralClass operator) {
//        if(1==1) return (IntegralClass) getCompatible(operator, true);

        if (this instanceof NumericClass || operator instanceof NumericClass) {
            int whole = getWhole() + operator.getWhole();
            int scale = getScale() + operator.getScale();

            return NumericClass.get(whole + scale, scale);
        } else {
            return getCompatible(operator, true);
        }
    }

    public IntegralClass getDivide(IntegralClass operator) {
//        if(1==1) return (IntegralClass) getCompatible(operator, true);

        if (this instanceof NumericClass || operator instanceof NumericClass) {
            int whole = getWhole() + operator.getScale();
            int scale = getScale() + operator.getWhole();
            if(Settings.get().isUseMaxDivisionLength())
                scale = Settings.get().getMaxNumericScale();

            return NumericClass.get(whole + scale, scale);
        } else {
            return getCompatible(operator, true);
        }
    }

    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        if (isNegative(readNumber(value)))
            return "(" + value.toString() + ")";
        else
            return value.toString();
    }
    protected abstract boolean isNegative(T value);
    public abstract boolean isPositive(T obj);

    public T readNumber(Object obj) {
        return read(obj);
    }

    public boolean isZero(Object object) {
        double doubleValue = readNumber(object).doubleValue();
        return doubleValue > -0.0005 && doubleValue < 0.0005;
    }

    @Override
    public boolean fixedSize() { // так как NumericClass не fixedSize, а getCompatible может "смешивать" другие Integral с NumericeClass'ами
        return false;
    }

    @Override
    public T parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws ParseException {
        return readDBF(dbfRecord.getNumber(fieldName));
    }

    @Override
    public T parseJSON(Object value) throws JSONException, ParseException {
        if(value == null || value == JSONObject.NULL || (value instanceof String && (value.equals("NaN") || value.equals("Infinity") || value.equals("-NaN") || value.equals("-Infinity"))))
            return null;
        if(!(value instanceof Number))
            throw new ParseException("Number is required");
        return readJSON(value);
    }

    @Override
    public T parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        if(formulaValue.getCellType().equals(CellType.NUMERIC))
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
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.END;
    }

    protected boolean isEmptyString(String s) {
        return s.trim().isEmpty();
    }

    @Override
    public String getInputType(FormInstanceContext context) {
        if(context.isMobile)
            return "number";
        return super.getInputType(context);
    }
}
