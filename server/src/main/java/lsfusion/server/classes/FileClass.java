package lsfusion.server.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExtInt;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.i18n.LocalizedString;
import org.apache.commons.net.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public abstract class FileClass extends DataClass<byte[]> {

    public final boolean multiple;
    public boolean storeName;

    protected FileClass(boolean multiple, boolean storeName) {
        super(LocalizedString.create("{classes.file}"));

        this.multiple = multiple;
        this.storeName = storeName;
    }

    public byte[] getDefaultValue() {
        return new byte[0];
    }

    public Class getReportJavaClass() {
        return Object.class;
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

    public byte[] read(Object value) {
        return (byte[]) value;
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

    public String formatString(byte[] value) {
        return value != null ? Base64.encodeBase64String(value) : null;
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

    public static List<byte[]> getMultipleFiles(byte[] val) {

        List<byte[]> result = new ArrayList<>();

        ByteArrayInputStream byteInStream = new ByteArrayInputStream(val);
        DataInputStream inStream = new DataInputStream(byteInStream);

        try {
            int cnt = inStream.readInt();
            for (int i = 0; i < cnt; i++) {
                int length = inStream.readInt();
                byte temp[] = new byte[length];
                inStream.readFully(temp);
                result.add(temp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static Map<String, byte[]> getMultipleNamedFiles(byte[] val, boolean multiple) {

        Map<String, byte[]> result = new HashMap<>();

        ByteArrayInputStream byteInStream = new ByteArrayInputStream(val);
        DataInputStream inStream = new DataInputStream(byteInStream);

        try {
            if (multiple) {
                int cnt = inStream.readInt();
                for (int i = 0; i < cnt; i++) {
                    int nameLength = inStream.readInt();
                    byte[] nameTemp = new byte[nameLength];
                    inStream.readFully(nameTemp);
                    int length = inStream.readInt();
                    byte temp[] = new byte[length];
                    inStream.readFully(temp);
                    result.put(new String(nameTemp), temp);
                }
            } else {
                int nameLength = inStream.readInt();
                byte[] nameTemp = new byte[nameLength];
                inStream.readFully(nameTemp);
                int length = inStream.readInt();
                byte temp[] = new byte[length];
                inStream.readFully(temp);
                result.put(new String(nameTemp), temp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public List<byte[]> getFiles(byte[] value) {
        if (value == null)
            return new ArrayList<>();

        if (!multiple && this instanceof DynamicFormatFileClass) { // обратная совместимость
            ByteArrayInputStream byteInStream = new ByteArrayInputStream(value);
            DataInputStream inStream = new DataInputStream(byteInStream);
            try {
                if (inStream.readInt() == 1) {
                    value = Arrays.copyOfRange(value, 4, value.length);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return multiple ? getMultipleFiles(value) : Collections.singletonList(value);
    }

    public Map<String, byte[]> getNamedFiles(byte[] value) {
        if (!storeName)
            throw new RuntimeException("Ошибка: файлы без имени");
        if (value == null)
            return new HashMap<>();

        return getMultipleNamedFiles(value, multiple);
    }

    public List<byte[]> getFiles(Object value) {
        return getFiles((byte[]) value);
    }

    public Map<String, byte[]> getNamedFiles(Object value) {
        return getNamedFiles((byte[]) value);
    }

    @Override
    public byte[] parseHTTP(Object o, Charset charset) throws ParseException {
        if(o instanceof String) {
            if(isParseNullValue((String)o))
                return null;
            o = BaseUtils.mergeFileAndExtension(((String)o).getBytes(charset), "unknown".getBytes());
        }

        if (BaseUtils.getExtension((byte[]) o).equals("null"))
            return null;
        return parseHTTPNotNull((byte[])o);
    }

    @Override
    public Object formatHTTP(byte[] value, Charset charset) {
        if(charset != null) {
            if (value == null)
                return getParseNullValue();
            return new String(BaseUtils.getFile(formatHTTPNotNull(value)), charset);
        }

        if(value == null)
            return BaseUtils.mergeFileAndExtension(new byte[]{}, "null".getBytes());
        return formatHTTPNotNull(value);
    }

    protected abstract byte[] parseHTTPNotNull(byte[] b);

    protected abstract byte[] formatHTTPNotNull(byte[] b);
}
