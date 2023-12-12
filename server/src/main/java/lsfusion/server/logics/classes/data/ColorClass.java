package lsfusion.server.logics.classes.data;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ColorClass extends DataClass<Color> {

    public final static ColorClass instance = new ColorClass();

    static {
        DataClass.storeClass(instance);
    }

    private ColorClass() { super(LocalizedString.create("{classes.color}")); }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ColorClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.COLOR;
    }

    @Override
    protected Class getReportJavaClass() {
        return Color.class;
    }

    @Override
    public DBType getDBType() {
        return IntegerClass.instance;
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
    public Color parseString(String s) throws ParseException {
        try {
            return Color.decode("#" + s.substring(s.length() - 6, s.length()));
        } catch (Exception e) {
            throw ParseException.propagateWithMessage("Error parsing string to color: " + s, e);
        }
    }

    @Override
    public String formatString(Color value, boolean ui) {
        return value == null ? null : String.valueOf(value.getRGB());
    }

    @Override
    public Object formatJSON(Color object) {
        return object.getRGB();
    }

    // it seems that SQL does the same conversion as above
    @Override
    public String formatJSONSource(String valueSource, SQLSyntax syntax) {
        return super.formatJSONSource(valueSource, syntax);
    }

    @Override
    public String getSID() {
        return "COLOR";
    }

    @Override
    public Color getDefaultValue() {
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

    public String getInputType(FormInstanceContext context) {
        return "color";
    }

    @Override
    public FlexAlignment getValueAlignment() {
        return FlexAlignment.CENTER;
    }
}
