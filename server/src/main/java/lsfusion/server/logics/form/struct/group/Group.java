package lsfusion.server.logics.form.struct.group;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

public class Group extends AbstractNode {

    private final String canonicalName;

    public final LocalizedString caption;

    private NFOrderSet<AbstractNode> children = NFFact.orderSet(true);

    private DebugInfo.DebugPoint debugPoint;

    public Group(String canonicalName, LocalizedString caption) {
        this.canonicalName = canonicalName;
        this.caption = caption;
    }
    
    public static final Group NOGROUP = new Group(null, null); 

    public boolean system = false;

    public boolean createContainer() {
        return !system;
    }

    private boolean isSimple;
    public void changeChildrenToSimple(Version version) {
        isSimple = true;
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

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        for(AbstractNode child : getChildrenListIt()) // getting children list automatically finalizes it
            if(child instanceof Group)
                child.finalizeAroundInit();
    }

    public void add(AbstractNode prop, Version version) {
        Group prevParent = prop.getNFParent(version);
        if (prevParent != null) {
            if (prevParent == this) // не только оптимизация, но и mutable логика
            {
                return;
            }
            prevParent.remove(prop, version);
        }
        children.add(prop, version);
        prop.setParent(this, version);
        if(isSimple)
            cleanParentActionOrPropertiesCaches();
    }

    @IdentityStartLazy
    public ImMap<ActionOrProperty, Integer> getIndexedPropChildren() { // оптимизация
        MExclMap<ActionOrProperty, Integer> mResult = MapFact.mExclMap();
        int count = 0;
        for (AbstractNode child : getChildrenListIt()) {
            count++;
            if (child instanceof ActionOrProperty) {
                mResult.exclAdd((ActionOrProperty) child, count); // can be not exclusive when the property is cached and then added several times to private group (see the whole commit for example)
            }
        }
        return mResult.immutable();
    }

    public void remove(AbstractNode prop, Version version) {
        children.remove(prop, version);
        prop.parent.set(null, version);
    }

    public boolean hasChild(ActionOrProperty prop) {
        return getActionOrProperties().contains(prop);
    }

    public boolean hasNFChild(ActionOrProperty prop, Version version) {
        for (AbstractNode child : getNFChildrenIt(version)) {
            if (child.hasNFChild(prop, version)) {
                return true;
            }
        }
        return false;
    }

    public void cleanAllActionOrPropertiesCaches() {
        actionOrProperties = null;

        for (AbstractNode child : getChildrenListIt())
            if(child instanceof Group)
                ((Group) child).cleanAllActionOrPropertiesCaches();
    }
    public void cleanParentActionOrPropertiesCaches() {
        actionOrProperties = null;

        Group parent = getParent();
        if(parent != null)
            parent.cleanParentActionOrPropertiesCaches();
    }

    private ImOrderSet<ActionOrProperty> actionOrProperties;

    @ManualLazy
    public ImOrderSet<ActionOrProperty> getActionOrProperties() {
        ImOrderSet<ActionOrProperty> result = actionOrProperties; // need this scheme for the proper concurrency (to guarantee that cache won't be dropped)
        if (result == null) {
            MOrderSet<ActionOrProperty> mResult = SetFact.mOrderSet();
            for (AbstractNode child : getChildrenListIt()) {
                mResult.addAll(child.getActionOrProperties());
            }
            result = mResult.immutableOrder();
            actionOrProperties = result;
        }

        return result;
    }
    
    public ImList<Group> getParentGroups() {
        MList<Group> mResult = ListFact.mList();
        fillParentGroups(mResult);
        return mResult.immutableList();        
    }
    public void fillParentGroups(MList<Group> mResult) {
        Group parent = getParent();
        if (parent != null) {
            parent.fillParentGroups(mResult);
        }
        mResult.add(this);
    }
    public ImList<Group> getChildGroups() {
        MList<Group> mResult = ListFact.mList();
        fillChildGroups(mResult);
        return mResult.immutableList();
    }
    public void fillChildGroups(MList<Group> mResult) {
        mResult.add(this);
        for (AbstractNode child : getChildrenListIt()) {
            if (child instanceof Group) {
                ((Group) child).fillChildGroups(mResult);
            }
        }
    }

    @Override
    public ImList<ActionOrPropertyClassImplement> calcActionOrProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses) {
        MList<ActionOrPropertyClassImplement> mResult = ListFact.mList();
        for (AbstractNode child : getChildrenListIt()) {
            mResult.addAll(child.getActionOrProperties(valueClasses, mapClasses));
        }
        return mResult.immutableList();
    }

    @Override
    public String toString() {
        return caption == null ? super.toString() : ThreadLocalContext.localize(caption);
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getName() {
        return CanonicalNameUtils.getName(canonicalName);
    }
    
    private String integrationSID;

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID != null ? integrationSID : getName();
    }
    
    // todo [dale]: Используется для идентификации групп свойств в reflection, желательно перевести на canonical names
    public String getSID() {
        return CanonicalNameUtils.toSID(canonicalName);
    }

    public boolean isNamed() {
        return canonicalName != null;
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }

    public DebugInfo.DebugPoint getDebugPoint() {
        return debugPoint;
    }
}
