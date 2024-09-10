package lsfusion.server.logics.form.struct.group;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.server.base.caches.ManualLazy;
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

    public static void cleanPropCaches() {
        hashProps.clear();
    }

    private static class CacheEntry {
        private final AbstractNode node;
        private final ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses;
        private final boolean isNoAny;

        private ImList<ActionOrPropertyClassImplement> result;

        public CacheEntry(AbstractNode node, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean isNoAny) {
            this.node = node;
            this.mapClasses = mapClasses;
            this.isNoAny = isNoAny;
        }

        public ImRevMap<ValueClassWrapper, ValueClassWrapper> map(CacheEntry entry) {
            if(!(mapClasses.size() == entry.mapClasses.size() && BaseUtils.hashEquals(node, entry.node) && BaseUtils.hashEquals(isNoAny, entry.isNoAny)))
                return null;

            MRevMap<ValueClassWrapper, ValueClassWrapper> mResult = MapFact.mRevMap();
            for(int i=0,size=mapClasses.size();i<size;i++) {
                ImSet<ValueClassWrapper> wrappers = mapClasses.getValue(i);
                ImSet<ValueClassWrapper> entryWrappers = entry.mapClasses.get(mapClasses.getKey(i));
                if(entryWrappers == null || wrappers.size() != entryWrappers.size())
                    return null;
                for(int j=0,sizeJ=wrappers.size();j<sizeJ;j++)
                    mResult.revAdd(wrappers.get(j), entryWrappers.get(j));
            }
            return mResult.immutableRev();
        }

        public int hash() {
            int result = 0;
            for(int i=0,size=mapClasses.size();i<size;i++) {
                result += mapClasses.getKey(i).hashCode() ^ mapClasses.getValue(i).size();
            }

            return 31 * (31 * result + node.hashCode()) + (isNoAny ? 1 : 0);
        }
    }
    final static LRUSVSMap<Integer, MAddCol<CacheEntry>> hashProps = new LRUSVSMap<>(LRUUtil.G2);

    @ManualLazy
    public ImList<ActionOrPropertyClassImplement> getActionOrProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean isNoAny) {
        CacheEntry entry = new CacheEntry(this, mapClasses, isNoAny); // кэширование
        int hash = entry.hash();
        MAddCol<CacheEntry> col = hashProps.get(hash);
        if(col == null) {
            col = ListFact.mAddCol();
            hashProps.put(hash, col);
        } else {
            synchronized (col) {
                for (CacheEntry cachedEntry : col.it()) {
                    final ImRevMap<ValueClassWrapper, ValueClassWrapper> map = cachedEntry.map(entry);
                    if (map != null) {
                        return cachedEntry.result.mapListValues((ActionOrPropertyClassImplement value) -> value.map(map));
                    }
                }
            }
        }

        ImList<ActionOrPropertyClassImplement> result = calcActionOrProperties(valueClasses, mapClasses, isNoAny);

        entry.result = result;
        synchronized (col) {
            col.add(entry);
        }

        return result;
    }

    public abstract ImList<ActionOrPropertyClassImplement> calcActionOrProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean isNoAny);
}
