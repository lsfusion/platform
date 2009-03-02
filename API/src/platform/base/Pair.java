package platform.base;

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 20.02.2009
 * Time: 11:01:38
 * To change this template use File | Settings | File Templates.
 */
public class Pair<Class1, Class2> {

    Class1 first;
    public Class2 second;

    public Pair(Class1 ifirst, Class2 isecond) {
        first = ifirst;
        second = isecond;
    }

    public String toString() { return first.toString(); }

}
