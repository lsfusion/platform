package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.postgresql.util.PGobject;

import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public abstract class AJSONClass extends FileBasedClass<String> implements DBType {

    public AJSONClass(LocalizedString caption) {
        super(caption);
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
    public DBType getDBType() {
        return this;
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
    public boolean isSafeType() {
        return false;
    }

    @Override
    public String parseString(String s) {
        return s;
    }

    @Override
    protected String parseHTTPNotNull(FileData o) {
        return new String(o.getRawFile().getBytes());
    }

    @Override
    protected FileData formatHTTPNotNull(String value) {
        return new FileData(new RawFileData(value.getBytes()), "json");
    }

    @Override
    protected String writePropNotNull(RawFileData value, String extension) {
        return new String(value.getBytes());
    }

    @Override
    public String read(Object value) {
        return value instanceof PGobject ? ((PGobject) value).getValue() : value instanceof String ? (String) value : null;
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    @Override
    public Class getReportJavaClass() {
        return String.class;
    }

    @Override
    public boolean isFlex() {
        return true;
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }
}