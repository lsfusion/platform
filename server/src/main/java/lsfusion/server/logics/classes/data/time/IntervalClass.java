package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.cell.IntervalValue;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.FormatClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.TextBasedClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import static lsfusion.base.DateConverter.*;

public abstract class IntervalClass<T> extends FormatClass<IntervalValue> {

    public static IntervalClass getInstance(String type) {
        switch (type) {
            case "DATE":
                return DateIntervalClass.instance;
            case "TIME":
                return TimeIntervalClass.instance;
            case "DATETIME":
                return DateTimeIntervalClass.instance;
            case "ZDATETIME":
                return ZDateTimeIntervalClass.instance;
        }
        return null;
    }

    protected IntervalClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    protected void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBigDecimal(num, ((IntervalValue) value).toBigDecimal());
    }

    @Override
    protected int getBaseDotNetSize() {
        return 0;
    }

    @Override
    public DBType getDBType() {
        return NumericClass.defaultNumeric;
    }

    @Override
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlDecimal";
    }

    @Override
    public String getDotNetRead(String reader) {
        return reader + ".ReadDecimal()";
    }

    @Override
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getSQL(SQLSyntax syntax) {
        return syntax.getIntervalSQL();
    }

    protected abstract Long parse(String date) throws ParseException;
    protected abstract Long parseUIString(String date, String pattern) throws ParseException;
    protected abstract String format(Long epoch);
    protected abstract String formatUI(Long epoch, String pattern);

    @Override
    public IntervalValue parseString(String s) throws ParseException {
        return (IntervalValue) parseInterval(s, this::parse);
    }

    @Override
    public IntervalValue parseUI(String value, String pattern) throws ParseException {
        return (IntervalValue) parseInterval(value, date -> parseUIString(date, pattern));
    }

    @Override
    public String formatString(IntervalValue obj) {
        return formatInterval(obj, this::format);
    }

    @Override
    public String formatUI(IntervalValue obj, String pattern) {
        return formatInterval(obj, epoch -> formatUI(epoch, pattern));
    }

    protected abstract String getSQLFrom(String source);
    protected abstract String getSQLTo(String source);

    @Override
    public String formatStringSource(String valueSource, SQLSyntax syntax) {
        return getSQLFrom(valueSource) + " || ' - ' || " + getSQLTo(valueSource);
    }

    @Override
    public IntervalValue read(Object value) {
        return IntervalValue.parseIntervalValue(value);
    }

    @Override
    public IntervalValue getDefaultValue() {
        long time = new Date().getTime();
        return new IntervalValue(time, time);
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.END;
    }

    @Override
    protected Class getReportJavaClass() {
        return IntervalValue.class;
    }
}