package lsfusion.server.classes;

import lsfusion.base.ExtInt;
import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.i18n.LocalizedString;
import org.apache.commons.net.util.Base64;

import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//import net.sourceforge.jtds.jdbc.BlobImpl;

public class ByteArrayClass extends DataClass<byte[]> {

    public final static ByteArrayClass instance = new ByteArrayClass();

    static {
        DataClass.storeClass(instance);
    }

    private ByteArrayClass() { super(LocalizedString.create("{classes.byte.array}")); }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ByteArrayClass?this:null;
    }

    public byte[] getDefaultValue() {
        return new byte[0];
    }

    public Class getReportJavaClass() {
        return new byte[0].getClass();
    }

    public byte getTypeID() {
        return Data.BYTEARRAY;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getByteArrayType();
    }
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlBinary";
    }
    public String getDotNetRead(String reader) {
        throw new UnsupportedOperationException();
    }
    public String getDotNetWrite(String writer, String value) {
        throw new UnsupportedOperationException();
    }
    public int getBaseDotNetSize() {
        throw new UnsupportedOperationException();
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getByteArraySQL();
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    public byte[] read(Object value) {
/*        if(value instanceof BlobImpl)
            try {
                return ((BlobImpl)value).getBytes(1, (int) ((BlobImpl)value).length());
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            }*/
        return (byte[])value;
    }

    public byte[] read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return set.getBytes(name);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public ExtInt getCharLength() {
        return ExtInt.UNLIMITED;
    }

    @Override
    public int getSize(byte[] value) {
        return value.length;
    }

    public byte[] parseString(String s) throws ParseException {
        return Base64.decodeBase64(s);
    }

    @Override
    public String formatString(byte[] value) {
        return value != null ? Base64.encodeBase64String(value) : null;
    }

    public String getSID() {
        return "ByteArrayClass";
    }

    @Override
    public String getParsedName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean calculateStat() {
        return false;
    }

    @Override
    public byte[] parseHTTP(Object o, Charset charset) throws ParseException {
        if(o instanceof String) {
            if (isParseNullValue((String) o))
                return null;
            return ((String) o).getBytes(charset);
        }
        
        if (((byte[]) o).length == 0)
            return null;
        return (byte[])o;
    }
    
    @Override
    public Object formatHTTP(byte[] value, Charset charset) {
        if(charset != null) {
            if (value == null)
                return getParseNullValue();
            return new String(value, charset);
        } 

        if (value == null)
            return new byte[]{};
        return value;
    }
}
