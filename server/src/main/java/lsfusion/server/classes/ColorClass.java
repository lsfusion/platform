package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;

public class ColorClass extends DataClass<Color> {

    public final static ColorClass instance = new ColorClass();

    static {
        DataClass.storeClass(instance);
    }

    private ColorClass() { super(ServerResourceBundle.getString("classes.color")); }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
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
    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getColorType();
    }
    @Override
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlInt32";
    }

    public String getDotNetRead(String reader) {
        return reader + ".ReadInt32()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }
    public int getBaseDotNetSize() {
        return 4;
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
    public Color parseString(String s) throws ParseException {
        try {
            return Color.decode("#" + s.substring(s.length() - 6, s.length()));
        } catch (Exception e) {
            throw new ParseException("error parsing string to color", e);
        }
    }

    @Override
    public String getSID() {
        return "COLOR";
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

    @Override
    public Color read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        int anInt = set.getInt(name);
        if(set.wasNull())
            return null;
        return read(anInt);
    }
}
