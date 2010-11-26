package platform.server.logics.property.group;

import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.ValueClassWrapper;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AbstractGroup extends AbstractNode implements ServerIdentitySerializable {


    public String caption;
    public boolean createContainer = true;
    private int ID;

    public AbstractGroup(int iID, String caption) {
        this.caption = caption;
        this.ID = iID;
    }

    public AbstractGroup(String caption) {
        this.ID = IDShift();
        this.caption = caption;
    }

    private static int currentID = 0;
    private int IDShift() {
        return currentID++;
    }

    List<AbstractNode> children = new ArrayList<AbstractNode>();
    public void add(AbstractNode prop) {
        children.add(prop);
        prop.parent = this;
    }

    protected void setChildOrder(LP<?> child, LP<?> childRel, boolean before) {
        setChildOrder(child.property, childRel.property, before);
    }

    protected void setChildOrder(Property child, Property childRel, boolean before) {

        int indProp = children.indexOf(child);
        int indPropRel = children.indexOf(childRel);

        if (before) {
            if (indPropRel < indProp) {
                for (int i = indProp; i >= indPropRel + 1; i--)
                    children.set(i, children.get(i - 1));
                children.set(indPropRel, child);
            }
        }
    }


    public List<ConcreteCustomClass> getClasses() {
        List<ConcreteCustomClass> result = new ArrayList<ConcreteCustomClass>();
        for (AbstractNode child : children)
            result.addAll(child.getClasses());
        return result;
    }

    public boolean hasChild(Property prop) {
        for (AbstractNode child : children)
            if(child.hasChild(prop))
                return true;
        return false;
    }

    public List<Property> getProperties() {
        List<Property> result = new ArrayList<Property>();
        for (AbstractNode child : children)
            result.addAll(child.getProperties());
        return result;
    }

    public Property getProperty(String sid) {
        for (AbstractNode child : children) {
            Property property = child.getProperty(sid);
            if (property != null) {
                return property;
            }
        }
        return null;
    }

    public List<PropertyClassImplement> getProperties(Collection<List<ValueClassWrapper>> classLists, boolean anyInInterface) {
        List<PropertyClassImplement> result = new ArrayList<PropertyClassImplement>();
        for (AbstractNode child : children)
            result.addAll(child.getProperties(classLists, anyInInterface));
        return result;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, getParent());

        List<ServerIdentitySerializable> serializableChildren = new ArrayList<ServerIdentitySerializable>();
        for (AbstractNode child : children) {
            if (child instanceof ServerIdentitySerializable) {
                serializableChildren.add((ServerIdentitySerializable) child);
            }
        }

        pool.serializeCollection(outStream, serializableChildren);
        pool.writeString(outStream, caption);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        //todo:
    }
}
