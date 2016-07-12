package lsfusion.base.col.lru;

import lsfusion.base.Pair;

public class MPair<Class1, Class2> {

    public Class1 first;
    public Class2 second;

    public MPair(Class1 first, Class2 second) {
        this.first = first;
        this.second = second;
    }

    // чтобы короче ситнаксис был
    public static <Class1,Class2> MPair<Class1,Class2> create(Class1 first, Class2 second) {
        return new MPair<Class1, Class2>(first, second);
    }

    public String toString() { return "(" + first.toString() + "," + second.toString() + ")"; }

    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
