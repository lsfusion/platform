package lsfusion.base.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SerializationUtil {
    public static void writeString(DataOutputStream outStream, String str) throws IOException {
        outStream.writeBoolean(str != null);
        if (str != null) {
            outStream.writeUTF(str);
        }
    }

    public static String readString(DataInputStream inStream) throws IOException {
        return inStream.readBoolean()
               ? inStream.readUTF()
               : null;
    }

    public static void writeInt(DataOutputStream outStream, Integer integer) throws IOException {
        outStream.writeBoolean(integer != null);
        if (integer != null) {
            outStream.writeInt(integer);
        }
    }

    public static Integer readInt(DataInputStream inStream) throws IOException {
        return inStream.readBoolean()
               ? inStream.readInt()
               : null;
    }

    public static void writeLong(DataOutputStream outStream, Long n) throws IOException {
        outStream.writeBoolean(n != null);
        if (n != null) {
            outStream.writeLong(n);
        }
    }

    public static Long readLong(DataInputStream inStream) throws IOException {
        return inStream.readBoolean()
               ? inStream.readLong()
               : null;
    }

}
