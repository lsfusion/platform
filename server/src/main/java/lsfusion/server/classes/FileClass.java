package lsfusion.server.classes;

import lsfusion.base.ExtInt;
import lsfusion.base.FileData;
import lsfusion.base.RawFileData;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.i18n.LocalizedString;
import org.apache.commons.net.util.Base64;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public abstract class FileClass<T> extends DataClass<T> {

    public final boolean multiple;
    public boolean storeName;

    protected FileClass(boolean multiple, boolean storeName) {
        super(LocalizedString.create("{classes.file}"));

        this.multiple = multiple;
        this.storeName = storeName;
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

    @Override
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

    @Override
    public ExtInt getCharLength() {
        return ExtInt.UNLIMITED;
    }

    protected abstract String getFileSID();

    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "") + (storeName ? "_StoreName" : "");
    }

    @Override
    public boolean calculateStat() {
        return false;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
        outStream.writeBoolean(storeName);
    }

    @Override
    public T parseHTTP(Object o, Charset charset) throws ParseException {
        if(o instanceof String) {
            if(isParseNullValue((String)o))
                return null;
            o = new FileData(new RawFileData(((String) o).getBytes(charset)), "unknown");
        }

        if (((FileData) o).getExtension().equals("null"))
            return null;
        return parseHTTPNotNull((FileData) o);
    }

    @Override
    public Object formatHTTP(T value, Charset charset) {
        if(charset != null) {
            if (value == null)
                return getParseNullValue();
            return new String(formatHTTPNotNull(value).getRawFile().getBytes(), charset);
        }

        if(value == null) 
            return new FileData(RawFileData.EMPTY, "null");
        return formatHTTPNotNull(value);
    }

    protected abstract T parseHTTPNotNull(FileData b);

    protected abstract FileData formatHTTPNotNull(T b);
}
