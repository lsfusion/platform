package platform.server.data.type;

import platform.server.data.sql.SQLSyntax;

public abstract class AbstractType<T> implements Type<T> {

    public boolean isSafeType(Object value) {
        return true;
    }

    public String getCast(String value, SQLSyntax syntax, boolean needLength) {
        return "CAST(" + value + " AS " + getDB(syntax) + ")";
    }

    public String getBinaryCast(String value, SQLSyntax syntax, boolean needLength) {
        int typeLength = getBinaryLength(syntax.isBinaryString());
        String castString = "CAST(" + value + " AS " + syntax.getBinaryType(typeLength) + ")";
        if(needLength && syntax.isBinaryString()) // потому как иначе СУБД зачем-то тримит строку
            castString = "lpad(" + castString + "," + typeLength + ")";
        return castString;
    }
}
