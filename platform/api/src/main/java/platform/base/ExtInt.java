package platform.base;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// в GWT есть симметричный класс
public class ExtInt {

    public final int value;  // -1, бесконечность

    public final static ExtInt ZERO = new ExtInt(0);
    public final static ExtInt UNLIMITED = new ExtInt(-1);

    public ExtInt(int value) {
        this.value = value;
    }

    public ExtInt sum(ExtInt ext) {
        if(isUnlimited())
            return this;
        if(ext.isUnlimited())
            return ext;

        return new ExtInt(getValue() + ext.getValue());
    }

    public ExtInt max(ExtInt ext) {
        if(isUnlimited())
            return this;
        if(ext.isUnlimited())
            return this;

        if(getValue() > ext.getValue())
            return this;
        return ext;
    }

    public boolean less(ExtInt ext) {
        if(isUnlimited())
            return false;

        if(ext.isUnlimited())
            return true;

        return getValue() < ext.getValue();
    }

    public boolean isUnlimited() {
        return value == -1;
    }

    public int getValue() {
        assert !isUnlimited();
        return value;
    }

    public int getAprValue() {
        if(isUnlimited())
            return 50;
        return getValue();
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(value);
    }

    public static ExtInt deserialize(DataInputStream inStream) throws IOException {
        return new ExtInt(inStream.readInt());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ExtInt && value == ((ExtInt) o).value;

    }

    @Override
    public int hashCode() {
        return value;
    }
}
