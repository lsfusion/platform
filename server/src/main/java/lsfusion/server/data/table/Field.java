package lsfusion.server.data.table;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.remote.serialization.BinarySerializable;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.type.exec.TypeEnvironment;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Function;

public abstract class Field extends TwinImmutableObject implements BinarySerializable {
    protected String name;
    public void setName(String name) {
        this.name = name;
    }
    
    public Type type;
    
    public String getName(SQLSyntax syntax) {
        return syntax.getFieldName(name);
    }
    public String getName() {
        return name;
    }

    public static <F extends Field> Type.Getter<F> typeGetter() {
        return key -> key.type;
    }

    public static <F extends Field> Function<F, Type> fnTypeGetter() {
        return value -> value.type;
    }

    public static <F extends Field> Function<F, String> nameGetter(final SQLSyntax syntax) {
        return (F value) -> value.getName(syntax);
    }

    public static <F extends Field> Function<F, String> nameGetter() {
        return Field::getName;
    }

    public final static SFunctionSet<Field> onlyKeys = element -> element instanceof KeyField;

    public final static SFunctionSet<Field> onlyProps = element -> element instanceof PropertyField;

    protected Field(String name,Type type) {
        this.name = name;
        this.type = type;
        assert type != null;
    }

    public static String getDeclare(ImOrderMap<String, Type> map, final SQLSyntax syntax, final TypeEnvironment typeEnv) {
        return map.mapOrderValues((Type value) -> value.getDB(syntax, typeEnv)).toString(" ", ",");
    }
    
    public String getDeclare(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return getName(syntax) + " " + type.getDB(syntax, typeEnv);
    }

    public String toString() {
        return name;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
        outStream.writeUTF(name);
        TypeSerializer.serializeType(outStream,type);
    }

    protected Field(DataInputStream inStream) throws IOException {
        name = inStream.readUTF();
        type = TypeSerializer.deserializeType(inStream);
    }

    public static Field deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readByte();
        if(type==0) return new KeyField(inStream);
        if(type==1) return new PropertyField(inStream);

        throw new IOException();
    }

    public abstract byte getType();

    public boolean calcTwins(TwinImmutableObject o) {
        return name.equals(((Field)o).name) && type.equals(((Field)o).type);
    }

    public int immutableHashCode() {
        return (getClass().hashCode() * 31 + name.hashCode()) * 31 + type.hashCode();
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        type.write(out);
    }
}
