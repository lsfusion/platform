package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

abstract class IntegralType<T> extends Type<T> {

    protected IntegralType(String iID) {
        super(iID);
    }

    public String getEmptyString() {
        return "0";
    }

    public Object getEmptyValue() {
        return 0;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

}
