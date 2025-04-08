package lsfusion.server.logics.classes.data;

import lsfusion.interop.classes.DataType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.expr.where.classes.data.MatchWhere;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.postgresql.util.PGobject;

import java.sql.*;

public class TSQueryClass extends DataClass<String> implements DBType {

    private TSQueryClass() {
        super(LocalizedString.create("{classes.tsvector}"));
    }

    public final static TSQueryClass instance = new TSQueryClass();

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
        return syntax.getTSQuery();
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
        return syntax.getTextSQL();
    }

    @Override
    public String parseString(String s) throws ParseException {
        return null;
    }

    @Override
    public String getSID() {
        return "TSQUERY";
    }

    @Override
    public String read(Object value) {
        return value instanceof PGobject ? ((PGobject) value).getValue() : value instanceof String ? (String) value : null;
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof TSQueryClass ? this : null;
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    @Override
    public byte getTypeID() {
        return DataType.TSQUERY;
    }

    @Override
    protected Class getReportJavaClass() {
        return String.class;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if(typeFrom instanceof StringClass) {
            String language = ThreadLocalContext.getBusinessLogics().getSearchLanguage();
            return MatchWhere.getPrefixSearchQuery(syntax, value, language);
        }
        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    public boolean isSafeType() {
        return false;
    }

    @Override
    public boolean isSafeString(Object value) {
        return false;
    }
}
