package lsfusion.server.logics.property.group;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyClassImplement;
import lsfusion.server.logics.property.ValueClassWrapper;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @IdentityLazy
    public ImMap<String, Integer> getIndexedPropChildren() { // оптимизация
        MExclMap<String, Integer> mResult = MapFact.mExclMap(children.size());
        int count = 0;
        for(AbstractNode child : children) {
            count++;
            if(child instanceof Property)
                mResult.exclAdd(((Property)child).getSID(), count);
        }
        return mResult.immutable();
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

    public ImOrderSet<Property> getProperties() {
        MOrderSet<Property> result = SetFact.mOrderSet();
        for (AbstractNode child : children)
            result.addAll(child.getProperties());
        return result.immutableOrder();
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

    public ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists, boolean anyInInterface) {
        MList<PropertyClassImplement> mResult = ListFact.mList();
        for (AbstractNode child : children)
            mResult.addAll(child.getProperties(classLists, anyInInterface));
        return mResult.immutableList();
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
