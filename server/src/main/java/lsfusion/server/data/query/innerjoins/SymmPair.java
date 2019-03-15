package lsfusion.server.data.query.innerjoins;

import lsfusion.base.Pair;

public class SymmPair<Class1, Class2> {

    public final Class1 first;
    public final Class2 second;

    public SymmPair(Class1 first, Class2 second) {
        this.first = first;
        this.second = second;
    }

    public String toString() { return "(" + first.toString() + "," + second.toString() + ")"; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SymmPair && ((first.equals(((SymmPair) o).first) && second.equals(((SymmPair) o).second)) || (first.equals(((SymmPair) o).second) && second.equals(((SymmPair) o).first)));

    }

    @Override
    public int hashCode() {
        return first.hashCode() + second.hashCode();
    }
}
