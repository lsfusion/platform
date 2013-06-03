package lsfusion.base;

public class GlobalInteger implements GlobalObject {
    int integer;

    public GlobalInteger(int integer) {
        this.integer = integer;
    }

    public int hashCode() {
        return integer;
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof GlobalInteger && integer == ((GlobalInteger)obj).integer;
    }
}
