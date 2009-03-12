package platform.server.data;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;

public abstract class Field {
    public String name;
    public Type type;

    Field(String iName,Type iType) {
        name = iName;
        type = iType;}

    public String getDeclare(SQLSyntax syntax) {
        return name + " " + type.getDB(syntax);
    }

    public String toString() {
        return name;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
        outStream.writeUTF(name);
        type.serialize(outStream);
    }

    protected Field(DataInputStream inStream) throws IOException {
        name = inStream.readUTF();
        type = Type.deserialize(inStream);
    }

    public static Field deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readByte();
        if(type==0) return new KeyField(inStream);
        if(type==1) return new PropertyField(inStream);

        throw new IOException();
    }

    abstract byte getType();
}
