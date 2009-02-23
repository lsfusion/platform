package platform.client.navigator;

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 21.02.2009
 * Time: 8:51:30
 * To change this template use File | Settings | File Templates.
 */
public class ClientNavigatorElement {

    public int ID;
    public String caption;

    public boolean hasChildren = false;
    boolean allowChildren() { return hasChildren; }

    public boolean isPrintForm = false;

    public String toString() { return caption; }

}
