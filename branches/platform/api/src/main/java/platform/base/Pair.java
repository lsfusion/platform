package platform.base;

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 20.02.2009
 * Time: 11:01:38
 * To change this template use File | Settings | File Templates.
 */
public class Pair<Class1, Class2> {

    public Class1 first;
    public Class2 second;

    public Pair(Class1 first, Class2 second) {
        this.first = first;
        this.second = second;
    }

    public String toString() { return first.toString(); }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Pair && first.equals(((Pair) o).first) && second.equals(((Pair) o).second);

    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }
}
