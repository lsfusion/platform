package platform.server.view.navigator;

import platform.server.logics.BusinessLogics;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class NavigatorElement<T extends BusinessLogics<T>> {

    public int ID;
    public String caption = "";

    public NavigatorElement(int iID, String icaption) { this(null, iID, icaption); }
    public NavigatorElement(NavigatorElement<T> parent, int iID, String icaption) {
        ID = iID;
        caption = icaption;

        if (parent != null)
            parent.add(this);
    }

    private NavigatorElement parent;
    NavigatorElement getParent() { return parent; }

    private List<NavigatorElement<T>> children = new ArrayList<NavigatorElement<T>>();
    Collection<NavigatorElement<T>> getChildren() { return children; }

    public Collection<NavigatorElement<T>> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList<NavigatorElement<T>>(children);

        Collection<NavigatorElement<T>> result = new ArrayList<NavigatorElement<T>>();
        fillChildren(result);
        return result;
    }

    private void fillChildren(Collection<NavigatorElement<T>> result) {

        if (result.contains(this))
            return;

        result.add(this);

        for (NavigatorElement child : children)
            child.fillChildren(result);
    }

    public void add(NavigatorElement<T> child) {
        children.add(child);
        child.parent = this;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    NavigatorElement<T> getNavigatorElement(int elementID) {

        if (ID == elementID) return this;

        for(NavigatorElement<T> child : children) {
            NavigatorElement<T> element = child.getNavigatorElement(elementID);
            if (element != null) return element;
        }

        return null;
    }
}
