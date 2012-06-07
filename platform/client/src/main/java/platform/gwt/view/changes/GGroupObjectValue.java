package platform.gwt.view.changes;

import platform.gwt.view.GGroupObject;
import platform.gwt.view.GObject;
import platform.gwt.view.changes.dto.GGroupObjectValueDTO;
import platform.gwt.view.changes.dto.ObjectDTO;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GGroupObjectValue extends HashMap<GObject, Object> implements Serializable {
    public GGroupObjectValue(GGroupObjectValue... clones) {
        super();
        for (GGroupObjectValue clone : clones) {
            putAll(clone);
        }
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

    public ObjectDTO[] getValues(GGroupObject group) {
        int i = 0;
        ObjectDTO[] values = new ObjectDTO[group.objects.size()];
        for (GObject object : group.objects) {
            values[i++] = new ObjectDTO(get(object));
        }
        return values;
    }

    public ObjectDTO[] getValuesDTO() {
        int i = 0;
        ObjectDTO[] values = new ObjectDTO[size()];
        for (GObject object : keySet()) {
            values[i++] = new ObjectDTO(get(object));
        }
        return values;
    }

    public GGroupObjectValueDTO getValueDTO() {
        GGroupObjectValueDTO value = new GGroupObjectValueDTO();
        for (Map.Entry<GObject, Object> entry : entrySet()) {
            value.put(entry.getKey().ID, new ObjectDTO(entry.getValue()));
        }
        return value;
    }

    public void removeAll(Collection<GObject> keys) {
        for(GObject key : keys)
            remove(key);
    }
}
