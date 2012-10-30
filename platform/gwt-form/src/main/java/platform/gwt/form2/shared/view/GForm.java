package platform.gwt.form2.shared.view;

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

    public GObject getObject(int id) {
        for (GGroupObject groupObject : groupObjects) {
            for (GObject object : groupObject.objects) {
                if (object.ID == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public GGroupObject getGroupObject(int id) {
        for (GGroupObject groupObject : groupObjects) {
            if (groupObject.ID == id) {
                return groupObject;
            }
        }
        return null;
    }

    private transient HashMap<Integer, GPropertyDraw> idProps;

    private HashMap<Integer, GPropertyDraw> getIDProps() {
        if (idProps == null) {
            idProps = new HashMap<Integer, GPropertyDraw>();
            for (GPropertyDraw property : propertyDraws) {
                idProps.put(property.ID, property);
            }
        }
        return idProps;
    }

    public GPropertyDraw getProperty(int id) {
//        return getIDProps().get(id);
        for (GPropertyDraw property : propertyDraws) {
            if (property.ID == id) {
                return property;
            }
        }
        return null;
    }

}
