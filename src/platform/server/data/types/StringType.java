package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

public class StringType extends Type<String> {

    int length;
    StringType(int iLength) {
        super("S"+iLength);
        length = iLength;
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof StringType && length==((StringType)obj).length;
    }

    public int hashCode() {
        return length;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getStringType(length);
    }

    Object getMinValue() {
        return "";
    }

    public String getEmptyString() {
        return "''";
    }

    public Object getEmptyValue() {
        return "";
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public String read(Object value) {
        return (String) value;
    }

    public boolean greater(Object value1, Object value2) {
        throw new RuntimeException("Java не умеет сравнивать строки");
    }
}
