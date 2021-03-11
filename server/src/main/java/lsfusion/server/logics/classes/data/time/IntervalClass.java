package lsfusion.server.logics.classes.data.time;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public abstract class IntervalClass extends DataClass<BigDecimal> {

    protected IntervalClass(LocalizedString caption) {
        super(caption);
    }

    public LocalDateTime getLocalDateTime(BigDecimal value, boolean from) {
        String object = String.valueOf(value);
        int indexOfDecimal = object.indexOf(".");

        LocalDateTime ldtFrom = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(object.substring(0, indexOfDecimal))), ZoneId.systemDefault());
        LocalDateTime ldtTo = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(object.substring(indexOfDecimal + 1))), ZoneId.systemDefault());

        return from ? ldtFrom : ldtTo;
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

    @Override
    public BigDecimal parseString(String s) throws ParseException {
        throw new ParseException("Error parsing interval");
    }

    @Override
    public BigDecimal read(Object value) {
        if (value == null)
            return null;
        return new BigDecimal(String.valueOf(value));
    }

    @Override
    public BigDecimal getDefaultValue() {
        return null;
    }

    @Override
    protected Class getReportJavaClass() {
        return BigDecimal.class;
    }
}