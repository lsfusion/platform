package lsfusion.server.logics.form;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.*;
import lsfusion.server.logics.form.interactive.MappingInterface;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;
import lsfusion.server.logics.form.struct.FormEntity;

import java.util.HashSet;
import java.util.Set;

public class ObjectMapping {
    private Version version; // form map version

    public final boolean extend;
    public final FormEntity addForm; // old
    public final ImRevMap<ServerIdentityObject, ServerIdentityObject> addObjects; // new -> old, entities (groups (and maybe objects), property, regular filters) + containers without parent
    public final boolean implicitAdd;
    private final MAddMap<ServerIdentityObject, ServerIdentityObject> addImplicitObjects = MapFact.mAddMap(MapFact.nullOrSame());

    private final Set<ServerIdentityObject> copied = new HashSet<>();

    private <T extends ServerIdentityObject<T, AP>, AP extends ServerIdentityObject<AP, ?>> T getAdd(T object) {
        assert !copied.contains(object);

        AP addParent = object.getAddParent(this);
        if(addParent != null) {
            AP recAddParent = (AP) getAdd((T) addParent); // actually T is AP but there is a recursive generics
            if(recAddParent != null)
                return object.getAddChild(recAddParent, this); // assert not null
            return null;
        }

        T addObject = (T) addObjects.get(object);
        if(addObject != null)
            return addObject;

        if(object instanceof FormEntity)
            return (T) addForm;

        return null;
    }

    public ObjectMapping(FormEntity addForm, ImRevMap<ServerIdentityObject, ServerIdentityObject> addObjects, boolean extend, Version version) {
        this.extend = extend;
        this.implicitAdd = true;
        this.addForm = addForm;
        this.addObjects = addObjects;

        this.version = version;
    }

    public int id() {
        return addForm.genID.id();
    }

    // identity get

    private final MAddExclMap<ServerIdentityObject, ServerIdentityObject> objectsMap = MapFact.mAddExclMap();
    private final Set<ServerIdentityObject> recursionGuard = SetFact.mAddRemoveSet();

    private ServerIdentityObject add;
    public <T extends ServerIdentityObject<T, AP>, AP extends ServerIdentityObject<AP, ?>> void put(T object, T mapped) {
        if(add != null) {
            mapped = (T) add;
            add = null;
        } else
            copied.add(mapped);
        objectsMap.exclAdd(object, mapped);
    }
    public <T extends ServerIdentityObject<T, AP>, AP extends ServerIdentityObject<AP, ?>> T getFinal(T object) {
        return (T) objectsMap.get(object);
    }
    public <T extends ServerIdentityObject<T, AP>, AP extends ServerIdentityObject<AP, ?>> T getIdentity(T object) {

//        synchronized by object mapping (form for getAdd)
        T result = (T) objectsMap.get(object);
        if(result != null)
            return result;

        synchronized (extend ? addForm : this) { // if extend we need to synchronize form for getAdd (and finding objects) / add, otherwise only inside this mapping
            result = (T) objectsMap.get(object);
            if(result != null)
                return result;

            result = getAdd(object);

            if(extend && implicitAdd) {
                // getAdd -> getNF* -> get -> eventually get here once again and initialize this object, so we need to triple check that
                T recResult = (T) objectsMap.get(object);
                if (recResult != null) {
                    assert result == null || recResult == result;
                    return recResult;
                }
            }

            if(result != null) {
                // it's tricky here, we need to call copy anyway to force cascade "get" calls (to register NF collections in related add objects before they get finalized)
                // the logics that any object can be obtained with NF* (and it will flush gets), or with direct links (and for that we need call get with that trick / hack)
                add = result;
                object.copy(this); // it addes to objectsMap in the constructor for the recursive links
            }

            if (result == null)
                result = object.copy(this);

            // have to be here not in constructor to have all NF fields initialized
            result.extend(object, this);

            result.add(object, this);
            return result;
        }
    }

    // non identity get

    public FormEntity.ExProperty get(FormEntity.ExProperty key) {
        return new FormEntity.ExProperty(key, this);
    }

    // change methods

    // static setter
    public <M extends MappingInterface<M>> M get(M from) {
        if(from == null)
            return null;

        return from.get(this);
    }
    public <M1 extends MappingInterface<M1>, M2 extends MappingInterface<M2>> ImMap<M1, M2> get(ImMap<M1, M2> from) {
        if(from == null)
            return null;

        return from.mapKeyValues(this::get, this::get);
    }
    public <M1, M2 extends MappingInterface<M2>> ImMap<M1, M2> gets(ImMap<M1, M2> from) {
        return from.mapValues(this::get);
    }
    public <M1, M2 extends MappingInterface<M2>> ImRevMap<M1, M2> gets(ImRevMap<M1, M2> from) {
        return from.mapRevValues((M2 value) -> get(value));
    }
    public <M extends MappingInterface<M>> ImOrderSet<M> get(ImOrderSet<M> from) {
        return from.mapOrderSetValues(this::get);
    }
    public <M extends MappingInterface<M>> ImList<M> get(ImList<M> from) {
        if(from == null)
            return null;

        return from.mapListValues(this::get);
    }

    public <X> void sets(NFProperty<X> to, NFProperty<X> from) {
        assert to != from;
        to.set(from, p -> p, version);
    }

    public <M extends MappingInterface<M>> void set(NFProperty<M> to, NFProperty<M> from) {
        assert to != from;
        to.set(from, this::get, version);
    }

    // collections

    public <M> void adds(NFSet<M> to, NFSet<M> from) {
        assert to != from;
        to.add(from, p -> p, version);
    }
    public <M extends MappingInterface<M>> void add(NFSet<M> to, NFSet<M> from) {
        assert to != from;
        to.add(from, this::get, version);
    }
    public <M extends MappingInterface<M>> void add(NFList<M> to, NFList<M> from) {
        assert to != from;
        to.add(from, this::get, version);
    }
    public <M extends MappingInterface<M>> void add(NFOrderSet<M> to, NFOrderSet<M> from) {
        assert to != from;
        to.add(from, this::get, version);
    }
    public <M extends MappingInterface<M>> void addl(NFOrderSet<ImList<M>> to, NFOrderSet<ImList<M>> from) {
        assert to != from;
        to.add(from, prop -> prop.mapItListValues(this::get), version);
    }
    public <M extends MappingInterface<M>> void add(NFComplexOrderSet<M> to, NFComplexOrderSet<M> from) {
        assert to != from;
        to.add(from, this::get, version);
    }
    public <M, V extends MappingInterface<V>> void add(NFMap<M, V> to, NFMap<M, V> from) {
        assert to != from;
        to.add(from, this::get, version);
    }
    public <M extends MappingInterface<M>, V> void add(NFOrderMap<M, V> to, NFOrderMap<M, V> from) {
        assert to != from;
        to.add(from, this::get, version);
    }
}
