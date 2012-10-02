package platform.server.classes;

import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public class ColorClass extends DataClass {

    public final static ColorClass instance = new ColorClass();

    static {
        DataClass.storeClass(instance);
    }

    public ColorClass() {
        super(ServerResourceBundle.getString("classes.color"));
    }

    @Override
    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ColorClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return Data.COLOR;
    }

    @Override
    protected Class getReportJavaClass() {
        return Color.class;
    }

    @Override
    public String getDB(SQLSyntax syntax) {
        return syntax.getColorType();
    }

    @Override
    public int getSQL(SQLSyntax syntax) {
        return syntax.getColorSQL();
    }

    @Override
    public boolean isSafeString(Object value) {
        return false;
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        return String.valueOf(((Color)value).getRGB());
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setInt(num, ((Color) value).getRGB());
    }

    @Override
    public Format getReportFormat() {
        throw new RuntimeException("not supported");
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            return Color.decode("#" + s.substring(s.length() - 6, s.length()));
        } catch (Exception e) {
            throw new ParseException("error parsing string to color", e);
        }
    }

    @Override
    public String getSID() {
        return "ColorClass";
    }

    @Override
    public Object getDefaultValue() {
        return Color.WHITE;
    }

    @Override
    public Color read(Object value) {
        if (value instanceof Integer) {
            return new Color((Integer) value);
        } if (value instanceof Color) {
            return (Color) value;
        } else {
            return null;
        }
    }
}
