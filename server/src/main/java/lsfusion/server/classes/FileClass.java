package lsfusion.server.classes;

import lsfusion.base.ExtInt;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.util.*;


public abstract class FileClass extends DataClass<byte[]> {

    public final boolean multiple;
    public boolean storeName;

    protected FileClass(boolean multiple, boolean storeName) {
        super(ServerResourceBundle.getString("classes.file"));

        this.multiple = multiple;
        this.storeName = storeName;
    }

    public Object getDefaultValue() {
        return new byte[0];
    }

    public Class getReportJavaClass() {
        return Object.class;
    }

    public Format getReportFormat() {
        return null;
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

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
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
        throw new RuntimeException("not supported");
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

        List<byte[]> result = new ArrayList<byte[]>();

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

        Map<String, byte[]> result = new HashMap<String, byte[]>();

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
            return new ArrayList<byte[]>();

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
            return new HashMap<String, byte[]>();

        return getMultipleNamedFiles(value, multiple);
    }

    public List<byte[]> getFiles(Object value) {
        return getFiles((byte[]) value);
    }

    public Map<String, byte[]> getNamedFiles(Object value) {
        return getNamedFiles((byte[]) value);
    }
}
