package lsfusion.server.logics.form.struct.group;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public abstract class AbstractNode extends ImmutableObject {

    protected NFProperty<Group> parent;
    // need to do it lazy to avoid keeping NFProperty forever for "private" properties
    @NFLazy
    private NFProperty<Group> getParentProperty() {
        if(parent == null)
            parent = NFFact.property(true);
        return parent;
    }
    public Group getParent() { return parent != null ? parent.get() : null; }
    public Group getNFParent(Version version) { return parent != null ? parent.getNF(version) : null; }
    public void setParent(Group group, Version version) {
        getParentProperty().set(group, version);
    }
    
    public void finalizeAroundInit() {
        if(parent != null)
            parent.finalizeChanges();
    }

    public abstract boolean hasChild(ActionOrProperty prop);

    public abstract boolean hasNFChild(ActionOrProperty prop, Version version);

    public abstract ImOrderSet<ActionOrProperty> getActionOrProperties();

    protected abstract ImList<ActionOrPropertyClassImplement> getActionOrProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, Version version);
}
