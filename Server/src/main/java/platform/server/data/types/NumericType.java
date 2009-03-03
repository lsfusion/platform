package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

class NumericType extends DoubleType {

    int length;
    int precision;
    NumericType(int iLength,int iPrecision) {
        super("N"+iLength+"P"+iPrecision);
        length = iLength;
        precision = iPrecision;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getNumericType(length,precision);
    }
}
