package lsfusion.server.logics.property.group;

import lsfusion.base.ImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.interfaces.NFProperty;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyClassImplement;
import lsfusion.server.logics.property.ValueClassWrapper;

import java.lang.ref.WeakReference;
import java.util.List;

public abstract class AbstractNode extends ImmutableObject {

    NFProperty<AbstractGroup> parent = NFFact.property(true);
    public AbstractGroup getParent() { return parent.get(); }
    public AbstractGroup getNFParent(Version version) { return parent.getNF(version); }

    public abstract boolean hasChild(Property prop);

    public abstract boolean hasNFChild(Property prop, Version version);

    public abstract ImOrderSet<Property> getProperties();

    public abstract ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists, boolean anyInInterface, Version version);

    public abstract List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList);
}
