package lsfusion.server.logics.property.group;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.caches.IdentityStartLazy;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyClassImplement;
import lsfusion.server.logics.property.ValueClassWrapper;

import java.util.ArrayList;
import java.util.List;

public class AbstractGroup extends AbstractNode {

    private final String sID;

    public final LocalizedString caption;

    private NFOrderSet<AbstractNode> children = NFFact.orderSet(true);

    public boolean createContainer = true;

    public AbstractGroup(String sID, LocalizedString caption) {
        this.sID = sID;
        this.caption = caption;
    }

    public void changeChildrenToSimple(Version version) {
        children = NFFact.simpleOrderSet(children.getNFOrderSet(version));
    }

    public ImSet<AbstractNode> getChildren() {
        return children.getSet();
    }

    public Iterable<AbstractNode> getChildrenIt() {
        return children.getIt();
    }

    public Iterable<AbstractNode> getChildrenListIt() {
        return children.getListIt();
    }

    public Iterable<AbstractNode> getNFChildrenIt(Version version) {
        return children.getNFIt(version);
    }

    public Iterable<AbstractNode> getNFChildrenListIt(Version version) {
        return children.getNFListIt(version);
    }

    public void add(AbstractNode prop, Version version) {
        AbstractGroup prevParent = prop.getNFParent(version);
        if (prevParent != null) {
            if (prevParent == this) // не только оптимизация, но и mutable логика
            {
                return;
            }
            prevParent.remove(prop, version);
        }
        children.add(prop, version);
        prop.parent.set(this, version);
    }

    @IdentityStartLazy
    public ImMap<Property, Integer> getIndexedPropChildren() { // оптимизация
        MExclMap<Property, Integer> mResult = MapFact.mExclMap();
        int count = 0;
        for (AbstractNode child : getChildrenListIt()) {
            count++;
            if (child instanceof Property) {
                mResult.exclAdd((Property) child, count);
            }
        }
        return mResult.immutable();
    }

    public void remove(AbstractNode prop, Version version) {
        children.remove(prop, version);
        prop.parent.set(null, version);
    }

    public boolean hasChild(Property prop) {
        for (AbstractNode child : getChildrenIt()) {
            if (child.hasChild(prop)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNFChild(Property prop, Version version) {
        for (AbstractNode child : getNFChildrenIt(version)) {
            if (child.hasNFChild(prop, version)) {
                return true;
            }
        }
        return false;
    }

    public ImOrderSet<Property> getProperties() {
        MOrderSet<Property> result = SetFact.mOrderSet();
        for (AbstractNode child : getChildrenListIt()) {
            result.addAll(child.getProperties());
        }
        return result.immutableOrder();
    }

    public List<AbstractGroup> getParentGroups() {
        List<AbstractGroup> result = new ArrayList<>();
        result.add(this);
        for (AbstractNode child : getChildrenListIt()) {
            if (child instanceof AbstractGroup) {
                result.add((AbstractGroup) child);
            }
            List<AbstractGroup> childGroups = new ArrayList<>();
            childGroups = child.fillGroups(childGroups);
            for (AbstractGroup c : childGroups) {
                if (!c.getChildren().isEmpty()) {
                    result.addAll(c.getParentGroups());
                } else {
                    result.add((c));
                }
            }
        }
        return result;
    }

    @Override
    protected ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean useObjSubsets, boolean anyInInterface, Version version) {
        MList<PropertyClassImplement> mResult = ListFact.mList();
        for (AbstractNode child : getNFChildrenListIt(version)) {
            mResult.addAll(child.getProperties(valueClasses, mapClasses, useObjSubsets, anyInInterface, version));
        }
        return mResult.immutableList();
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        for (AbstractNode child : getChildrenListIt()) {
            if (child instanceof AbstractGroup) {
                groupsList.add((AbstractGroup) child);
            }
        }
        return groupsList;
    }

    public String getSID() {
        return sID;
    }

    @Override
    public String toString() {
        return caption == null ? super.toString() : caption.getSourceString();
    }
}
