package lsfusion.server.logics.classes.data;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.file.FileBasedClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.net.util.Base64;

import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//import net.sourceforge.jtds.jdbc.BlobImpl;

public class ByteArrayClass extends FileBasedClass<RawFileData> implements DBType {

    public final static ByteArrayClass instance = new ByteArrayClass();

    static {
        DataClass.storeClass(instance);
    }

    private ByteArrayClass() { super(LocalizedString.create("{classes.byte.array}")); }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ByteArrayClass?this:null;
    }

    public RawFileData getDefaultValue() {
        return RawFileData.EMPTY;
    }

    public Class getReportJavaClass() {
        return RawFileData.class;
    }

    public byte getTypeID() {
        return DataType.BYTEARRAY;
    }

    @Override
    public DBType getDBType() {
        return this;
    }
    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
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

    public RawFileData read(Object value) {
        if(value instanceof byte[])
            return new RawFileData((byte[]) value);
/*        if(value instanceof BlobImpl)
            try {
                return ((BlobImpl)value).getBytes(1, (int) ((BlobImpl)value).length());
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            }*/
        return (RawFileData) value;
    }

    public RawFileData read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        byte[] result = set.getBytes(name);
        if(result != null)
            return new RawFileData(result);
        return null;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, value != null ? ((RawFileData) value).getBytes() : null);
    }

    @Override
    public ExtInt getCharLength() {
        return ExtInt.UNLIMITED;
    }

    @Override
    public int getSize(RawFileData value) {
        return value.getLength();
    }

    public RawFileData parseString(String s) {
        return new RawFileData(Base64.decodeBase64(s));
    }

    @Override
    public String formatString(RawFileData value, boolean ui) {
        return value != null ? Base64.encodeBase64StringUnChunked(value.getBytes()) : null;
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
    protected RawFileData parseHTTPNotNull(FileData b) {
        return b.getRawFile();
    }

    @Override
    protected FileData formatHTTPNotNull(RawFileData value) {
        return new FileData(value, "bytea");
    }
}
