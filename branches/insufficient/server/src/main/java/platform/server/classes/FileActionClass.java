package platform.server.classes;

import platform.base.Pair;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class FileActionClass extends ActionClass {

    private boolean multiple;
    private boolean custom;
    private String filterDescription;
    private String filterExtensions[];
    private final String sid;

    public FileActionClass(boolean multiple, boolean custom, String filterDescription, String filterExtensions) {
        this.multiple = multiple;
        this.custom = custom;
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions.split(" ");
        sid = formatSID(multiple, custom, filterDescription, filterExtensions);
    }

    private static String formatSID(boolean multiple, boolean custom, String filterDescription, String filterExtensions) {
        return "FileActionClass[multiple=" + multiple + ", custom=" + custom + ", filterDescription=" + filterDescription + "," + filterExtensions + "]";
    }

    @Override
    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof FileActionClass ? this : null;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
        outStream.writeBoolean(custom);
        outStream.writeUTF(filterDescription);
        outStream.writeInt(filterExtensions.length);
        for (String extension : filterExtensions) {
            outStream.writeUTF(extension);
        }
    }

    @Override
    protected Class getReportJavaClass() {
        return byte[].class;
    }

    @Override
    public byte getTypeID() {
        return Data.FILEACTION;
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
//        if(value==null) return null;
        return value;
    }

    private static HashMap<String, FileActionClass> instances = new HashMap<String, FileActionClass>();

    public static FileActionClass getDefinedInstance(boolean multiple, String description, String extensions) {
        return getInstance(multiple, false, description, extensions);
    }

    public static FileActionClass getCustomInstance(boolean multiple) {
        return getInstance(multiple, true, "", "");
    }

    public static FileActionClass getInstance(boolean multiple, boolean custom, String description, String extensions) {
        String sid = formatSID(multiple, custom, description, extensions);
        FileActionClass instance = instances.get(sid);
        if (instance == null) {
            instance = new FileActionClass(multiple, custom, description, extensions);
            instances.put(sid, instance);
            DataClass.storeClass(instance.getSID(), instance);
        }
        return instance;
    }

    public String getSID() {
        return sid;
    }

    public ArrayList<byte[]> getFiles(Object value) {
        ArrayList<byte[]> result = new ArrayList<byte[]>();
        byte val[] = (byte[]) value;

        if ((!multiple) && (!custom)) {
            result.add(val);
        } else {
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
        }

        return result;
    }
}