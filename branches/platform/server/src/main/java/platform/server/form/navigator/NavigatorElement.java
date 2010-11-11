package platform.server.form.navigator;

import platform.base.BaseUtils;
import platform.base.IdentityObject;
import platform.server.logics.BusinessLogics;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NavigatorElement<T extends BusinessLogics<T>> extends IdentityObject {

    public String caption = "";

    public NavigatorElement() {
        
    }
    public NavigatorElement(int iID, String icaption) { this(null, iID, icaption); }
    public NavigatorElement(NavigatorElement<T> parent, int ID, String caption) {
        this.ID = ID;
        this.caption = caption;

        if (parent != null) {
            this.parent = parent;
            parent.add(this);
        }
    }

    private NavigatorElement<T> parent;
    public NavigatorElement<T> getParent() { return parent; }

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

    public void replaceChild(NavigatorElement<T> from, NavigatorElement<T> to) {
        BaseUtils.replaceListElements(children, from, to);
    }

    public void add(NavigatorElement<T> child) {
        children.add(child);
        child.parent = this;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public NavigatorElement<T> getNavigatorElement(int elementID) {

        if (getID() == elementID) return this;

        for(NavigatorElement<T> child : children) {
            NavigatorElement<T> element = child.getNavigatorElement(elementID);
            if (element != null) return element;
        }

        return null;
    }

    public byte getTypeID() {
        return 1;
    }
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        outStream.writeInt(getID());
        outStream.writeUTF(caption);
        outStream.writeBoolean(hasChildren());
    }
}
