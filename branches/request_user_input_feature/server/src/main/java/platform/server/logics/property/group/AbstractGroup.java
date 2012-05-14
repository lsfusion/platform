package platform.server.logics.property.group;

import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.ValueClassWrapper;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class AbstractGroup extends AbstractNode implements ServerIdentitySerializable {


    public String caption;
    public boolean createContainer = true;
    private int ID;
    private String sID;

    public AbstractGroup(String sID, int iID, String caption) {
        this.sID = sID;
        this.caption = caption;
        this.ID = iID;
    }

    public AbstractGroup(String sID, String caption) {
        this.sID = sID;
        this.caption = caption;
        this.ID = IDShift();
    }

    private static int currentID = 0;
    private int IDShift() {
        return currentID++;
    }

    public Set<AbstractNode> children = new LinkedHashSet<AbstractNode>();
    public void add(AbstractNode prop) {
        if (prop.getParent() != null)
            prop.getParent().remove(prop);
        children.add(prop);
        prop.parent = this;
    }

    public void remove(AbstractNode prop) {
        children.remove(prop);
        prop.parent = null;
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

    public List<AbstractGroup> getParentGroups() {
        List<AbstractGroup> result = new ArrayList<AbstractGroup>();
        if (this instanceof AbstractGroup)
            result.add(this);
        for (AbstractNode child : children) {
            if (child instanceof AbstractGroup)
                result.add((AbstractGroup) child);
            List<AbstractGroup> childGroups = new ArrayList<AbstractGroup>();
            childGroups = child.fillGroups(childGroups);
            for (AbstractGroup c : childGroups) {
                if (!c.children.isEmpty())
                    result.addAll(c.getParentGroups());
                else if (c instanceof AbstractGroup)
                    result.add((c));
            }
        }
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

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        for (AbstractNode child : children)
            if (child instanceof AbstractGroup)  {
                groupsList.add((AbstractGroup) child);
            }
        return groupsList;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public String getSID() {
        return sID;
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
