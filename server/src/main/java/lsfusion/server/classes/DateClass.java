package lsfusion.server.classes;

import lsfusion.server.data.type.Type;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import lsfusion.base.DateConverter;
import lsfusion.base.ExtInt;
import lsfusion.base.SystemUtils;
import lsfusion.interop.Data;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.ServerResourceBundle;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;

public class DateClass extends DataClass<Date> {

    public final static DateClass instance = new DateClass();

    static {
        DataClass.storeClass(instance);
    }

    private DateClass() { super(ServerResourceBundle.getString("classes.date")); }

    public String toString() {
        return ServerResourceBundle.getString("classes.date");
    }

    public int getPreferredWidth() { return 70; }

    public Format getReportFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public Class getReportJavaClass() {
        return java.util.Date.class;
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        if (!super.fillReportDrawField(reportField))
            return false;

        reportField.alignment = HorizontalAlignEnum.RIGHT.getValue();
        return true;
    }

    public byte getTypeID() {
        return Data.DATE;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DateClass?this:null;
    }

    public Object getDefaultValue() {
        return DateConverter.getCurrentDate();
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getDateType();
    }
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlDateTime";
    }

    public String getDotNetRead(String reader) {
        return "DateTime.FromBinary("+reader+".ReadInt64())";
    }

    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" +value + ".ToBinary());";
    }

    public int getBaseDotNetSize() {
        return 8;
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getDateSQL();
    }

    public Date read(Object value) {
        DateConverter.assertDateToSql((java.util.Date)value);
        return DateConverter.safeDateToSql((java.util.Date)value);
    }

    @Override
    public Date read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
//        Date read = super.read(set, syntax, name);
        Date date = set.getDate(name);
//        assert BaseUtils.hashEquals(read, date);
        return date;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setDate(num, (Date)value);
    }

    @Override
    public boolean isSafeType() {
        return false;
    }

    @Override
    public ExtInt getCharLength() {
        return new ExtInt(25);
    }

    public boolean isSafeString(Object value) {
        return false;
    }
    
    public String getString(Object value, SQLSyntax syntax) {
        return "{d '" + value + "'}";
    }

    public static DateFormat getDateFormat() {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        dateFormat.setTimeZone(SystemUtils.getCurrentTimeZone());  // todo [dale]: Нужно брать таймзону из бизнес-логики
        return dateFormat;
    }

    public Date parseString(String s) throws ParseException {
        try {
            DateConverter.assertDateToSql(getDateFormat().parse(s));
            return DateConverter.safeDateToSql(getDateFormat().parse(s));
        } catch (Exception e) {
            throw new ParseException("error parsing date", e);
        }
    }

    public static String format(Date date) {
        return getDateFormat().format(date);
    }

    public String getSID() {
        return "DATE";
    }

    @Override
    public Object getInfiniteValue(boolean min) {
        return DateConverter.dateToSql(new java.util.Date(min ? 0 : Long.MAX_VALUE));
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }

    @Override
    public Object castValue(Object object, Type typeFrom, SQLSyntax syntax) {
        return syntax.fixDate((Date) object);
    }

    @Override
    public boolean useIndexedJoin() {
        return true;
    }
}
