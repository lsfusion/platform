package platform.server.data.types;

import platform.server.data.classes.DataClass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TypeSerializer {

    public static void serialize(DataOutputStream outStream, Type type) throws IOException {
        if(type instanceof DataClass) {
            outStream.writeBoolean(false);
            ((DataClass)type).serialize(outStream);
        } else
            outStream.writeBoolean(true);
    }

    public static Type deserialize(DataInputStream inStream) throws IOException {
        if(inStream.readBoolean())
            return ObjectType.instance;
        else
            return DataClass.deserialize(inStream);
    }
}
