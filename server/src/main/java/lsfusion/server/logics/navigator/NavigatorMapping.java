package lsfusion.server.logics.navigator;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;

import java.util.IdentityHashMap;
import java.util.Map;

// what the elements and the windows of a navigator are in its copy, as ObjectMapping is for a form's copy : a copy is
// made as it is first asked for, and the source's NF properties and collections are copied at the copy's version. Only
// the code of a customization makes a copy, of the final navigator and in one thread, so none of the form's monitor here.
// The copy keeps it, and once the copy is final it is only read (getFinal)
public class NavigatorMapping {
    private final Version version;

    private final Map<Object, Object> copies = new IdentityHashMap<>();

    public NavigatorMapping(Version version) {
        this.version = version;
    }

    // an element's copy asks for its parent's as it is made - a final property is mapped as it is copied -, so that one is
    // made on the way; the parents make no cycle and the children are mapped as they are first read, so a copy is kept
    // once it is made (ObjectMapping keeps it as it is constructed, for the links back)
    public <M extends NavigatorMappingInterface<? super M>> M get(M object) {
        if (object == null)
            return null;

        M copy = (M) copies.get(object);
        if (copy == null) {
            copy = (M) object.copy(this);
            copies.put(object, copy);
        }
        return copy;
    }

    // the copy made, nothing made on the way
    public <M extends NavigatorMappingInterface<? super M>> M getFinal(M object) {
        return (M) copies.get(object);
    }

    public <X> void sets(NFProperty<X> to, NFProperty<X> from) {
        assert to != from;
        to.set(from, p -> p, version);
    }

    public <X> void adds(NFList<X> to, NFList<X> from) {
        assert to != from;
        to.add(from, p -> p, version);
    }

    public <M extends NavigatorMappingInterface<? super M>> void set(NFProperty<M> to, NFProperty<M> from) {
        assert to != from;
        to.set(from, this::get, version);
    }

    public <M extends NavigatorMappingInterface<? super M>> void add(NFOrderSet<M> to, NFOrderSet<M> from) {
        assert to != from;
        to.add(from, this::get, version);
    }

    public <M extends NavigatorMappingInterface<? super M>> void add(NFComplexOrderSet<M> to, NFComplexOrderSet<M> from) {
        assert to != from;
        to.add(from, this::get, version);
    }
}
