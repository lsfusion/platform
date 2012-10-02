package platform.server.classes;

import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public abstract class FileClass extends DataClass<byte[]> {

    public final boolean multiple;

    protected FileClass(boolean multiple) {
        super(ServerResourceBundle.getString("classes.file"));

        this.multiple = multiple;
    }

    protected FileClass(DataInputStream inStream) throws IOException {
        super(ServerResourceBundle.getString("classes.file"));

        this.multiple = inStream.readBoolean();
    }

    public Object getDefaultValue() {
        return new byte[0];
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public Format getReportFormat() {
        return null;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getByteArrayType();
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

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported");
    }

    public byte[] parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    protected abstract String getFileSID();
    public String getSID() {
        return getFileSID() + (multiple?"_Multiple":"");
    }

    @Override
    public boolean calculateStat() {
        return false;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
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

    public List<byte[]> getFiles(byte[] value) {
        if(value==null)
            return new ArrayList<byte[]>();
            
        if(!multiple && this instanceof DynamicFormatFileClass) { // обратная совместимость со Сколково так что так криво
            ByteArrayInputStream byteInStream = new ByteArrayInputStream(value);
            DataInputStream inStream = new DataInputStream(byteInStream);
            try {
                if(inStream.readInt()==1) {
                    value = Arrays.copyOfRange(value, 4, value.length);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return multiple? getMultipleFiles(value) : Collections.singletonList(value);
    }

    public List<byte[]> getFiles(Object value) {
        return getFiles((byte[])value);
    }
}
