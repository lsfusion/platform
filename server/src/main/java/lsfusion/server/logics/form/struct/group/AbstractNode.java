package lsfusion.server.logics.form.struct.group;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public abstract class AbstractNode extends ImmutableObject {

    NFProperty<AbstractGroup> parent = NFFact.property(true);
    public AbstractGroup getParent() { return parent.get(); }
    public AbstractGroup getNFParent(Version version) { return parent.getNF(version); }
    
    public void finalizeAroundInit() {
        parent.finalizeChanges();
    }

    public abstract boolean hasChild(ActionOrProperty prop);

    public abstract boolean hasNFChild(ActionOrProperty prop, Version version);

    public abstract ImOrderSet<ActionOrProperty> getProperties();
    
    protected abstract ImList<ActionOrPropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, Version version);
    
    public ImList<ActionOrPropertyClassImplement> getProperties(ImSet<ValueClassWrapper> classLists, Version version) {
        return getProperties(classLists, classLists.group(new BaseUtils.Group<ValueClass, ValueClassWrapper>() { // для "кэширования" mapClasses так как очень часто вызывается
            public ValueClass group(ValueClassWrapper key) {
                return key.valueClass;
            }}), version);
    }
    
    public ImList<ActionOrPropertyClassImplement> getProperties(ValueClass valueClass, Version version) {
        return getProperties(valueClass != null ? SetFact.singleton(new ValueClassWrapper(valueClass)) : SetFact.<ValueClassWrapper>EMPTY(), version);
    }
}
