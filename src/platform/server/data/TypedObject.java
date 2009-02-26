package platform.server.data;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;

public class TypedObject {
    public Object value;
    public Type type;

    public TypedObject(Object iValue, Type iType) {
        value = iValue;
        type = iType;
    }

    public static String getString(Object Value, Type Type, SQLSyntax Syntax) {
        if(Value==null)
            return Type.NULL;
        else
            return Type.getString(Value,Syntax);
    }

    public String getString(SQLSyntax Syntax) {
        return getString(value, type,Syntax);
    }

    public String toString() {
        if(value ==null)
            return Type.NULL;
        else
            return value.toString();
    }

    public Object multiply(int mult) {
        if(value instanceof Boolean)
            return ((Boolean)value?1:0);
        else
            return ((Integer)value)*mult;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof TypedObject && ((value==null && ((TypedObject)o).value==null) || (value!=null && value.equals(((TypedObject)o).value)));
    }

    public int hashCode() {
        return (value != null ? value.hashCode() : 0);
    }
}
