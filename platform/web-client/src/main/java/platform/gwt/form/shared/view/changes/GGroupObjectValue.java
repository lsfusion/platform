package platform.gwt.form.shared.view.changes;

import platform.gwt.form.shared.view.GObject;

import java.io.Serializable;
import java.util.*;

public class GGroupObjectValue extends HashMap<Integer, Object> implements Serializable {
    public static final GGroupObjectValue EMPTY = new GGroupObjectValue();

    public static final List<GGroupObjectValue> SINGLE_EMPTY_KEY_LIST = Arrays.asList(EMPTY);

    public GGroupObjectValue() {
    }

    public GGroupObjectValue(GGroupObjectValue... clones) {
        super();
        for (GGroupObjectValue clone : clones) {
            putAll(clone);
        }
    }

    public GGroupObjectValue(Integer object, int value) {
        put(object, value);
    }

    @Override
    public String toString() {
        String caption = "[";
        for (Map.Entry<Integer, Object> entry : entrySet()) {
            if (caption.length() > 1) {
                caption += ",";
            }

            caption += entry.getKey() + "=" + entry.getValue();
        }

        caption += "]";
        return caption;
    }

    public void removeAll(Collection<GObject> keys) {
        for (GObject key : keys) {
            remove(key.ID);
        }
    }
}
