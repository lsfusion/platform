package lsfusion.server.logics.classes.data.time;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import static lsfusion.base.DateConverter.*;

public abstract class IntervalClass<T> extends DataClass<BigDecimal> {

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
        statement.setBigDecimal(num, (BigDecimal) value);
    }

    @Override
    protected int getBaseDotNetSize() {
        return 0;
    }

    @Override
    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getIntervalType();
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

    @Override
    public boolean isSafeString(Object value) {
        return false;
    }

    protected abstract Long parse(String date) throws ParseException;
    protected abstract String format(Long epoch);

    @Override
    public BigDecimal parseString(String s) throws ParseException {
        return (BigDecimal) parseInterval(s, this::parse);
    }

    @Override
    public String formatString(BigDecimal obj) {
        return formatInterval(obj, this::format);
    }

    @Override
    public BigDecimal read(Object value) {
        int length = value instanceof String ? ((String) value).split("[.]").length : 0;
        return value == null || length > 2  ? null : new BigDecimal(String.valueOf(value));
    }

    @Override
    public BigDecimal getDefaultValue() {
        long time = new Date().getTime();
        return new BigDecimal(time + "." + time);
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    @Override
    protected Class getReportJavaClass() {
        return BigDecimal.class;
    }
}