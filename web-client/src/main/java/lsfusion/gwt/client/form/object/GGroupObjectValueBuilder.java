package lsfusion.gwt.client.form.object;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

public class GGroupObjectValueBuilder {

    private final TreeMap<Integer, Serializable> key = new TreeMap<>();

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

    public GGroupObjectValueBuilder removeAll(Collection<GObject> objects) {
        for (GObject obj : objects) {
            key.remove(obj.ID);
        }
        return this;
    }

    public GGroupObjectValueBuilder put(int ID, Serializable value) {
        key.put(ID, value);
        return this;
    }

    public GGroupObjectValueBuilder putAll(GGroupObjectValue value) {
        for (int i = 0; i < value.size(); ++i) {
            key.put(value.getKey(i), value.getValue(i));
        }
        return this;
    }

    public GGroupObjectValue toGroupObjectValue() {
        return key.isEmpty() ? GGroupObjectValue.EMPTY : new GGroupObjectValue(key);
    }
}
