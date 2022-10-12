package lsfusion.server.logics.classes.data;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SystemClass extends DataClass<Long> {

    public final static SystemClass instance = new SystemClass();

    static {
        DataClass.storeClass(instance);
    }

    private SystemClass() { super(LocalizedString.create("{classes.system}")); }
    
    public byte getTypeID() {
        throw new RuntimeException("not supported yet");
    }

    public Class getReportJavaClass() {
        return idClass.getReportJavaClass();
    }

    public Long getDefaultValue() {
        return idClass.getDefaultValue();
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof SystemClass?this:null; 
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return idClass.getDB(syntax, typeEnv);
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return idClass.getDotNetType(syntax, typeEnv);
    }

    public String getDotNetRead(String reader) {
        return idClass.getDotNetRead(reader);
    }
    public String getDotNetWrite(String writer, String value) {
        return idClass.getDotNetWrite(writer, value);
    }

    @Override
    public int getBaseDotNetSize() {
        return idClass.getBaseDotNetSize();
    }

    public int getSQL(SQLSyntax syntax) {
        return idClass.getSQL(syntax);
    }

    public Long read(Object value) {
        return idClass.read(value);
    }

    @Override
    public Long read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return idClass.read(set, syntax, name);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        idClass.writeParam(statement, num, value, syntax);
    }

    public boolean isSafeString(Object value) {
        return idClass.isSafeString(value);
    }

    private final static LongClass idClass = ObjectType.idClass;
    
    public String getString(Object value, SQLSyntax syntax) {
        return idClass.getString(value, syntax);
    }

    public ExtInt getCharLength() {
        return idClass.getCharLength();
    }

    public Long parseString(String s) {
        throw new RuntimeException("not supported");
    }

    public String getSID() {
        return "SystemClass";
    }
}
