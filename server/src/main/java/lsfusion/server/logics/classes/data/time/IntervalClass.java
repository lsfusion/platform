package lsfusion.server.logics.classes.data.time;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public abstract class IntervalClass extends DataClass<BigDecimal> {

    protected static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
    protected static final DateFormat DATE_TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    protected static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.MEDIUM);

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
        String[] dates = s.split(" - ");
        Long epochFrom = parse(dates[0]);
        Long epochTo = parse(dates[1]);
        return epochFrom < epochTo ? new BigDecimal(epochFrom + "." + epochTo) : null;
    }

    @Override
    public String formatString(BigDecimal value) {
        return format(getIntervalPart(value, true)) + " - " + format(getIntervalPart(value, false));
    }

    private Long getIntervalPart(Object o, boolean from) {
        String object = String.valueOf(o);
        int indexOfDecimal = object.indexOf(".");
        return Long.parseLong(indexOfDecimal < 0 ? object : from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1));
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