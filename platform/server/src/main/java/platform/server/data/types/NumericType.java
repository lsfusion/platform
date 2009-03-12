package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

import java.io.DataOutputStream;
import java.io.IOException;

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

    byte getType() {
        return 5;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(length);
        outStream.writeInt(precision);
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof NumericType && length==((NumericType)obj).length && precision==((NumericType)obj).precision;
    }

    public int hashCode() {
        return 31 * length + precision;
    }
}
