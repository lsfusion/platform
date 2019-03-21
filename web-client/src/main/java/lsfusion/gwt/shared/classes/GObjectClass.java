package lsfusion.gwt.shared.classes;

import java.util.ArrayList;

public class GObjectClass implements GClass {

    public boolean concreate;
    public long ID;
    public String caption;
    public ArrayList<GObjectClass> children;

    @SuppressWarnings("UnusedDeclaration")
    public GObjectClass() {
    }

    public GObjectClass(long id, boolean concreate, String caption, ArrayList<GObjectClass> children) {
        this.ID = id;
        this.concreate = concreate;
        this.caption = caption;
        this.children = children;
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public String toString() {
        return caption;
    }
}
