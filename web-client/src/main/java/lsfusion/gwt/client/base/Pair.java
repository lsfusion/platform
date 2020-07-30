package lsfusion.gwt.client.base;

import java.io.Serializable;

public class Pair<Class1, Class2> implements Serializable {

    public Class1 first;
    public Class2 second;

    public Pair() {
    }

    public Pair(Class1 first, Class2 second) {
        this.first = first;
        this.second = second;
    }

    public String toString() { return "(" + first.toString() + "," + second.toString() + ")"; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Pair && first.equals(((Pair) o).first) && second.equals(((Pair) o).second);

    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }
}
