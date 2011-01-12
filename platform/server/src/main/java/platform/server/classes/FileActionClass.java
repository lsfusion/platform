package platform.server.classes;

import platform.base.Pair;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class FileActionClass extends ActionClass {

    private String filterDescription;
    private String filterExtensions[];
    private final String sid;

    private FileActionClass(String filterDescription, String filterExtensions) {
        this.filterExtensions = filterExtensions.split(" ");
        this.filterDescription = filterDescription;
        sid = "FileActionClass[" + filterDescription + "," + filterExtensions + "]";
    }

    @Override
    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof FileActionClass ? this : null;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

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

    private static HashMap<Pair<String, String>, FileActionClass> instances = new HashMap<Pair<String, String>, FileActionClass>();
    public static FileActionClass getInstance(String description, String extensions) {
        Pair<String, String> key = new Pair<String, String>(description, extensions);
        FileActionClass instance = instances.get(key);
        if (instance == null) {
            instance = new FileActionClass(description, extensions);
            instances.put(key, instance);
            DataClass.storeClass(instance.getSID(), instance);
        }

        return instance;
    }

    public String getSID() {
        return sid;
    }
}