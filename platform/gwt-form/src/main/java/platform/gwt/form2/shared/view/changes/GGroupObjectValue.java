package platform.gwt.form2.shared.view.changes;

import platform.gwt.form2.shared.view.GObject;
import platform.gwt.form2.shared.view.changes.dto.GGroupObjectValueDTO;

import java.io.Serializable;
import java.util.*;

public class GGroupObjectValue extends HashMap<GObject, Object> implements Serializable {
    public static final GGroupObjectValue EMPTY = new GGroupObjectValue() {
        @Override
        public void putAll(Map<? extends GObject, ?> m) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public Object put(GObject key, Object value) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException("not supported");
        }
    };

    public static final List<GGroupObjectValue> SINGLE_EMPTY_KEY_LIST = Arrays.asList(EMPTY);

    public GGroupObjectValue(GGroupObjectValue... clones) {
        super();
        for (GGroupObjectValue clone : clones) {
            putAll(clone);
        }
    }

    public GGroupObjectValue(GObject object, int value) {
        put(object, value);
    }

    @Override
    public String toString() {
        String caption = "[";
        for (Map.Entry<GObject, Object> entry : entrySet()) {
            if (caption.length() > 1) {
                caption += ",";
            }

            caption += entry.getKey().getCaption() + "=" + entry.getValue();
        }

        caption += "]";
        return caption;
    }

    public GGroupObjectValueDTO getValueDTO() {
        GGroupObjectValueDTO value = new GGroupObjectValueDTO();
        for (Map.Entry<GObject, Object> entry : entrySet()) {
            value.put(entry.getKey().ID, entry.getValue());
        }
        return value;
    }

    public void removeAll(Collection<GObject> keys) {
        for(GObject key : keys)
            remove(key);
    }
}
