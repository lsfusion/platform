package platform.gwt.form.shared.view;

import platform.gwt.form.shared.view.changes.dto.GFormChangesDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class GForm implements Serializable {
    public String sessionID;

    public String caption;

    public GContainer mainContainer;
    public ArrayList<GTreeGroup> treeGroups = new ArrayList<GTreeGroup>();
    public ArrayList<GGroupObject> groupObjects = new ArrayList<GGroupObject>();
    public ArrayList<GPropertyDraw> propertyDraws = new ArrayList<GPropertyDraw>();
    public ArrayList<GRegularFilterGroup> regularFilterGroups = new ArrayList<GRegularFilterGroup>();
    public LinkedHashMap<GPropertyDraw, Boolean> defaultOrders = new LinkedHashMap<GPropertyDraw, Boolean>();

    public boolean allowScrollSplits;

    private transient HashMap<Integer, GPropertyDraw> idProps;
    private transient HashMap<Integer, GObject> idObjects;

    public GFormChangesDTO initialFormChanges;
    public GFormUserPreferences userPreferences;

    public GGroupObject getGroupObject(int id) {
        for (GGroupObject groupObject : groupObjects) {
            if (groupObject.ID == id) {
                return groupObject;
            }
        }
        return null;
    }

    public GGroupObject getGroupObject(String sID) {
        for (GGroupObject groupObject : groupObjects) {
            if (groupObject.getSID().equals(sID)) {
                return groupObject;
            }
        }
        return null;
    }

    public GObject getObject(int id) {
        GObject obj;
        if (idObjects == null) {
            idObjects = new HashMap<Integer, GObject>();
            obj = null;
        } else {
            obj = idObjects.get(id);
        }

        if (obj == null) {
            OBJECTS:
            for (GGroupObject groupObject : groupObjects) {
                for (GObject object : groupObject.objects) {
                    if (object.ID == id) {
                        obj = object;
                        idObjects.put(id, object);
                        break OBJECTS;
                    }
                }
            }
        }
        return obj;
    }

    public GPropertyDraw getProperty(int id) {
        GPropertyDraw prop;
        if (idProps == null) {
            idProps = new HashMap<Integer, GPropertyDraw>();
            prop = null;
        } else {
            prop = idProps.get(id);
        }

        if (prop == null) {
            for (GPropertyDraw property : propertyDraws) {
                if (property.ID == id) {
                    prop = property;
                    break;
                }
            }
        }
        return prop;
    }

}
