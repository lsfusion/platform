package platform.gwt.view.changes;

import platform.gwt.view.GForm;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.dto.GFormChangesDTO;
import platform.gwt.view.changes.dto.GGroupObjectValueDTO;
import platform.gwt.view.changes.dto.ObjectDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GFormChanges implements Serializable {
    public final HashMap<GGroupObject, GGroupObjectValue> objects = new HashMap<GGroupObject, GGroupObjectValue>();
    public final HashMap<GGroupObject, ArrayList<GGroupObjectValue>> gridObjects = new HashMap<GGroupObject, ArrayList<GGroupObjectValue>>();
    public final HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Object>> properties = new HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Object>>();
    public final HashSet<GPropertyDraw> panelProperties = new HashSet<GPropertyDraw>();
    public final HashSet<GPropertyDraw> dropProperties = new HashSet<GPropertyDraw>();

    public static GFormChanges remap(GForm form, GFormChangesDTO dto) {
        GFormChanges remapped = new GFormChanges();
        for (Map.Entry<Integer, ArrayList<GGroupObjectValueDTO>> e : dto.gridObjects.entrySet()) {
            remapped.gridObjects.put(form.getGroupObject(e.getKey()), remapGroupObjectValues(form, e.getValue()));
        }

        for (Map.Entry<Integer, HashMap<GGroupObjectValueDTO, ObjectDTO>> e : dto.properties.entrySet()) {
            remapped.properties.put(form.getProperty(e.getKey()), remapGroupObjectValueMap(form, e.getValue()));
        }

        for (Integer propertyID : dto.panelProperties) {
            remapped.panelProperties.add(form.getProperty(propertyID));
        }

        for (Integer propertyID : dto.dropProperties) {
            remapped.dropProperties.add(form.getProperty(propertyID));
        }

        for (Map.Entry<Integer, GGroupObjectValueDTO> e : dto.objects.entrySet()) {
            remapped.objects.put(form.getGroupObject(e.getKey()), remapGroupObjectValue(form, e.getValue()));
        }

        return remapped;
    }

    private static HashMap<GGroupObjectValue, Object> remapGroupObjectValueMap(GForm form, HashMap<GGroupObjectValueDTO, ObjectDTO> values) {
        HashMap<GGroupObjectValue, Object> res = new HashMap<GGroupObjectValue, Object>();
        for (Map.Entry<GGroupObjectValueDTO, ObjectDTO> e : values.entrySet()) {
            res.put(remapGroupObjectValue(form, e.getKey()), e.getValue().getValue());
        }
        return res;
    }

    private static ArrayList<GGroupObjectValue> remapGroupObjectValues(GForm form, ArrayList<GGroupObjectValueDTO> values) {
        ArrayList<GGroupObjectValue> res = new ArrayList<GGroupObjectValue>();
        for (GGroupObjectValueDTO key : values) {
            res.add(remapGroupObjectValue(form, key));
        }
        return res;
    }

    private static GGroupObjectValue remapGroupObjectValue(GForm form, GGroupObjectValueDTO key) {
        GGroupObjectValue remapped = new GGroupObjectValue();
        for (Map.Entry<Integer, ObjectDTO> e : key.entrySet()) {
            remapped.put(form.getObject(e.getKey()), e.getValue().getValue());
        }
        return remapped;
    }
}
