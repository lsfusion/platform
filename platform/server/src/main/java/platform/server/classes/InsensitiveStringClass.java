package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.util.ArrayList;
import java.util.Collection;

public class InsensitiveStringClass extends StringClass {

    public String toString() {
        return ServerResourceBundle.getString("classes.insensitive.string")+" " + length;
    }

    private InsensitiveStringClass(int length) {
        super(length);
    }

    public byte getTypeID() {
        return Data.INSENSITIVESTRING;
    }

    public DataClass getCompatible(DataClass compClass) {
        if (!(compClass instanceof StringClass)) {
            return null;
        }
        return length >= ((StringClass) compClass).length ? this : compClass;
    }

    private static Collection<InsensitiveStringClass> strings = new ArrayList<InsensitiveStringClass>();

    public static InsensitiveStringClass get(int length) {
        for (InsensitiveStringClass string : strings) {
            if (string.length == length) {
                return string;
            }
        }
        InsensitiveStringClass string = new InsensitiveStringClass(length);
        strings.add(string);
        DataClass.storeClass(string);
        return string;
    }

    public String getSID() {
        return "InsensitiveStringClass_" + length;
    }

    @Override
    public InsensitiveStringClass extend(int times) {
        return get(length * times);
    }
}
