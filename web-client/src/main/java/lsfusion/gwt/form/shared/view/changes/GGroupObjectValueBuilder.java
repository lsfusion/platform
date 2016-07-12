package lsfusion.gwt.form.shared.view.changes;

import lsfusion.gwt.form.shared.view.GObject;

import java.util.Collection;
import java.util.TreeMap;

public class GGroupObjectValueBuilder {

    private final TreeMap<Integer, Object> key = new TreeMap<Integer, Object>();

    public GGroupObjectValueBuilder() {
    }

    public GGroupObjectValueBuilder(GGroupObjectValueBuilder... builders) {
        for (GGroupObjectValueBuilder builder : builders) {
            key.putAll(builder.key);
        }
    }

    public GGroupObjectValueBuilder(GGroupObjectValue... values) {
        for (GGroupObjectValue value : values) {
            putAll(value);
        }
    }

    public GGroupObjectValueBuilder(int key, int value) {
        this.key.put(key, value);
    }

    public GGroupObjectValueBuilder removeAll(Collection<GObject> objects) {
        for (GObject obj : objects) {
            key.remove(obj.ID);
        }
        return this;
    }

    public GGroupObjectValueBuilder put(Integer k, Object value) {
        key.put(k, value);
        return this;
    }

    public GGroupObjectValueBuilder putAll(GGroupObjectValue value) {
        for (int i = 0; i < value.size(); ++i) {
            key.put(value.getKey(i), value.getValue(i));
        }
        return this;
    }

    public GGroupObjectValueBuilder remove(Integer k) {
        key.remove(k);
        return this;
    }

    public void clear() {
        key.clear();
    }

    public GGroupObjectValue toGroupObjectValue() {
        return key.isEmpty() ? GGroupObjectValue.EMPTY : new GGroupObjectValue(key);
    }
}
