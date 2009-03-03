package platform.server.data.types;

import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.NullExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;

import java.util.HashSet;
import java.util.Set;

public abstract class Type<T> {

    public String ID;
    Type(String iID) {
        ID = iID;
    }

    public static IntegerType integer = new IntegerType();
    public static LongType longType = new LongType();
    public static DoubleType doubleType = new DoubleType();
    public static BitType bit = new BitType();
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

    abstract public String getString(Object value, SQLSyntax syntax);

    public abstract T read(Object value);

    public abstract boolean greater(Object value1,Object value2);
}
