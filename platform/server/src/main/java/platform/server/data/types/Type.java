package platform.server.data.types;

import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.NullExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;

import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class Type<T> {

    public String ID;
    Type(String iID) {
        ID = iID;
    }

    public static IntegerType integer = new IntegerType();
    public static LongType longType = new LongType();
    public static DoubleType doubleType = new DoubleType();
    public static TextType text = new TextType();
    public static Type bytes = new ByteArrayType();
    public static Type bit = new BitType();
    public static IntegerType object;
    public static Type system;

    public static String NULL = "NULL";

    public static StringType string(int length) {
        StringType result = new StringType(length);
        types.add(result);
        return result;
    }
    public static NumericType numeric(int length,int precision) {
        NumericType result = new NumericType(length,precision);
        types.add(result);
        return result;
    }
    static Set<Type> types = new HashSet<Type>();

    static {
        object = integer;
        system = integer;

        types.add(integer);
        types.add(longType);
        types.add(doubleType);
        types.add(bit);
    }

    public abstract String getDB(SQLSyntax syntax);

    abstract Object getMinValue();
    public abstract String getEmptyString();
    public abstract Object getEmptyValue();
    public ValueExpr getEmptyValueExpr() {
        return new ValueExpr(0,this);
    }

    public AndExpr getExpr(Object value) {
        if(value==null)
            return new NullExpr(this);
        else
            return new ValueExpr(value,this);
    }

    ValueExpr getMinValueExpr() {
        return new ValueExpr(getMinValue(),this);
    }

    abstract public boolean isString(Object value);
    abstract public String getString(Object value, SQLSyntax syntax);

    public abstract T read(Object value);

    public abstract boolean greater(Object value1,Object value2);

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
    }

    public static Type deserialize(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if(type==0) return integer;
        if(type==1) return longType;
        if(type==2) return doubleType;
        if(type==3) return bit;
        if(type==4) return string(inStream.readInt());
        if(type==5) return numeric(inStream.readInt(),inStream.readInt());

        throw new IOException();
    }

    abstract byte getType();

    abstract public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException;
}
