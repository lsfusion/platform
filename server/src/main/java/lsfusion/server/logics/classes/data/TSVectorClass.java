package lsfusion.server.logics.classes.data;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class TSVectorClass extends DataClass<Array> implements DBType {

    private TSVectorClass() {
        super(LocalizedString.create("{classes.tsvector}"));
    }

    public final static TSVectorClass instance = new TSVectorClass();

    @Override
    protected void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setObject(num, value, Types.OTHER);
    }

    @Override
    protected int getBaseDotNetSize() {
        return 0;
    }

    @Override
    public DBType getDBType() {
        return this;
    }
    @Override
    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getTSVector();
    }

    @Override
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDotNetRead(String reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDotNetWrite(String writer, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSQL(SQLSyntax syntax) {
        return Types.ARRAY;
    }

    @Override
    public Array parseString(String s) throws ParseException {
        return null;
    }

    @Override
    public String getSID() {
        return "TSVECTOR";
    }

    @Override
    public Array read(Object value) {
        return (Array) value;
    }

    @Override
    public Array read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        Array array = set.getArray(name);
        if(set.wasNull())
            return null;
        return readResult(array);
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof TSVectorClass ? this : null;
    }

    @Override
    public Array getDefaultValue() {
        return null;
    }

    @Override
    public byte getTypeID() {
        return DataType.TSVECTOR;
    }

    @Override
    protected Class getReportJavaClass() {
        return String.class;
    }

    @Override
    public boolean isSafeType() {
        return false;
    }

    @Override
    public boolean isSafeString(Object value) {
        return false;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if(typeFrom instanceof StringClass) {
            String language = Settings.get().getTsVectorDictionaryLanguage();
            return "to_tsvector('" + language + "'::regconfig, " + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }
}
