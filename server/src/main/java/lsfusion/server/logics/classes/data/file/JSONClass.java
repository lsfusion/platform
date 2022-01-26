package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class JSONClass extends DataClass<String> {

    public JSONClass() {
        super(LocalizedString.create("{classes.json}"));
    }

    public final static JSONClass instance = new JSONClass();

    static {
        DataClass.storeClass(instance);
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setObject(num, value, Types.OTHER);
    }

    @Override
    protected int getBaseDotNetSize() {
        return 400;
    }

    @Override
    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getJSON();
    }

    @Override
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlString";
    }

    @Override
    public String getDotNetRead(String reader) {
        return reader + ".ReadString()";
    }

    @Override
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getSQL(SQLSyntax syntax) {
        return syntax.getTextSQL();
    }

    @Override
    public boolean isSafeString(Object value) {
        if(value == null)
            return false;
        assert value instanceof String;
        return true;
    }

    @Override
    public String parseString(String s) {
        return s;
    }

    @Override
    public String formatString(String value) {
        return value;
    }

    @Override
    public String getSID() {
        return "JSON";
    }

    @Override
    public String read(Object value) {
        return value instanceof PGobject ? ((PGobject) value).getValue() : value instanceof String ? (String) value : null;
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof JSONClass ? this : null;
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    @Override
    public byte getTypeID() {
        return DataType.JSON;
    }

    @Override
    public Class getReportJavaClass() {
        return String.class;
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }
}