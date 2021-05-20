package lsfusion.server.logics.classes.data.time;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static lsfusion.base.DateConverter.formatInterval;
import static lsfusion.base.DateConverter.parseIntervalString;

public abstract class IntervalClass extends DataClass<BigDecimal> {

    protected static final DateTimeFormatter DATE_FORMATTER= DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    protected static final DateTimeFormatter Z_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

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

    protected abstract Long parse(String date);
    protected abstract String format(Long epoch);

    @Override
    public BigDecimal parseString(String s) throws ParseException {
        return (BigDecimal) parseIntervalString(s, this::parse);
    }

    @Override
    public String formatString(BigDecimal obj) {
        return formatInterval(obj, this::format);
    }

    @Override
    public BigDecimal read(Object value) {
        int length = value instanceof String ? ((String) value).split("[.]").length : 0;
        return value == null || length > 2  ? getDefaultValue() : new BigDecimal(String.valueOf(value));
    }

    @Override
    public BigDecimal getDefaultValue() {
        long l = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        return new BigDecimal(l + "." + l);
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