package lsfusion.server.logics.classes.data.file;

import lsfusion.base.Result;
import lsfusion.base.file.FileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.AStringClass;
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
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if (typeFrom instanceof FileClass)
            return getCastFromStatic(((FileClass<?>) typeFrom).getCastToStatic(value));

        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    public abstract String getCastFromStatic(String value);
    public abstract String getCastToStatic(String value);


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
        return AStringClass.isDBSafeString(value);
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        return AStringClass.getDBString(value);
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
    protected String parseHTTPNotNull(FileData o, String charsetName, String fileName) {
        return ExternalUtils.encodeFileData(o, charsetName);
    }

    @Override
    protected FileData formatHTTPNotNull(String value, Charset charset, Result<String> fileName) {
        return ExternalUtils.decodeFileData(value, charset.name(), "json");
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
}