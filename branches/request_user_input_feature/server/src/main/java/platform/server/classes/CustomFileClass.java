package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CustomFileClass extends FileClass{

    public final static CustomFileClass instance = new CustomFileClass();
    private final static String sid = "CustomClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected CustomFileClass() {}

    public String toString() {
        return ServerResourceBundle.getString("classes.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof CustomFileClass ? this : null;
    }

    public byte getTypeID() {
        return Data.CUSTOMFILECLASS;
    }

    public String getSID() {
        return sid;
    }

    public String getExtensions() {
        return "*.*";
    }

    @Override
    public boolean isCustom() {
        return true;
    }

    public static ArrayList<byte[]> getFiles(byte[] val) {

        ArrayList<byte[]> result = new ArrayList<byte[]>();

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
}
