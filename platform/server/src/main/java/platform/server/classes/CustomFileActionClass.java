package platform.server.classes;

import platform.base.Pair;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class CustomFileActionClass extends ActionClass {

    public final static CustomFileActionClass instance = new CustomFileActionClass();
    private final static String sid = "CustomFileActionClass";

    static {
        DataClass.storeClass(sid, instance);
    }


    private CustomFileActionClass() {
    }

    @Override
    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof CustomFileActionClass ? this : null;
    }

    @Override
    protected Class getReportJavaClass() {
        return byte[].class;
    }

    @Override
    public byte getTypeID() {
        return Data.CUSTOMFILEACTION;
    }

    @Override
    public Object getDefaultValue() {
        return new byte[0];
    }

    @Override
    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return null;
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public Object read(Object value) {
        return value;
    }

    public String getSID() {
        return sid;
    }
}