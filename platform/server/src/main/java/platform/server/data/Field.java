package platform.server.data;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Field extends TwinImmutableObject {
    public String name;
    public Type type;

    private final static Type.Getter<Field> typeGetter = new Type.Getter<Field>() {
        public Type getType(Field key) {
            return key.type;
        }
    };
    public static <F extends Field> Type.Getter<F> typeGetter() {
        return (Type.Getter<F>) typeGetter;
    }
    
    private final static GetValue<String, Field> nameGetter = new GetValue<String, Field>() {
        public String getMapValue(Field value) {
            return value.name;
        }
    };

    public static <F extends Field> GetValue<String, F> nameGetter() {
        return (GetValue<String, F>) nameGetter;
    }

    protected Field(String name,Type type) {
        this.name = name;
        this.type = type;
    }

    public static String getDeclare(ImOrderMap<String, Type> map, final SQLSyntax syntax) {
        return map.mapOrderValues(new GetValue<String, Type>() {
            public String getMapValue(Type value) {
                return value.getDB(syntax);
            }}).toString(" ", ",");
    }
    
    public String getDeclare(SQLSyntax syntax) {
        return name + " " + type.getDB(syntax);
    }

    public String toString() {
        return name;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
        outStream.writeUTF(name);
        TypeSerializer.serializeType(outStream,type);
    }

    protected Field(DataInputStream inStream, int version) throws IOException {
        name = inStream.readUTF();
        type = TypeSerializer.deserializeType(inStream, version);
    }

    public static Field deserialize(DataInputStream inStream, int version) throws IOException {
        int type = inStream.readByte();
        if(type==0) return new KeyField(inStream, version);
        if(type==1) return new PropertyField(inStream, version);

        throw new IOException();
    }

    abstract byte getType();

    public boolean twins(TwinImmutableObject o) {
        return name.equals(((Field)o).name) && type.equals(((Field)o).type);
    }

    public int immutableHashCode() {
        return (getClass().hashCode() * 31 + name.hashCode()) * 31 + type.hashCode();
    }
}
