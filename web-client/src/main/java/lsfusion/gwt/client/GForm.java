package lsfusion.gwt.client;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.*;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyDrawOrPivotColumn;
import lsfusion.gwt.client.form.property.async.GAsyncEventExec;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;

public class GForm implements Serializable {
    public String sessionID;

    public String sID;
    
    public String canonicalName;

    public String creationPath;
    public String path;

    public ArrayList<GFormScheduler> formSchedulers = new ArrayList<>();
    public Map<GFormEvent, GAsyncEventExec> asyncExecMap = new HashMap<>();

    public GContainer mainContainer;
    public HashSet<GTreeGroup> treeGroups = new HashSet<>();
    public ArrayList<GGroupObject> groupObjects = new ArrayList<>();
    public ArrayList<GPropertyDraw> propertyDraws = new ArrayList<>();
    public ArrayList<GRegularFilterGroup> regularFilterGroups = new ArrayList<>();
    public LinkedHashMap<GPropertyDraw, Boolean> defaultOrders = new LinkedHashMap<>();

    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> pivotColumns = new ArrayList<>();
    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> pivotRows = new ArrayList<>();
    public ArrayList<GPropertyDraw> pivotMeasures = new ArrayList<>();

    // caches for faster form changes transformation
    private final transient NativeHashMap<Integer, GPropertyDraw> idProps = new NativeHashMap<>();
    private final transient NativeHashMap<Integer, GObject> idObjects = new NativeHashMap<>();
    private final transient NativeHashMap<Integer, GGroupObject> idGroupObjects = new NativeHashMap<>();
    private final transient NativeHashMap<Integer, GContainer> idContainers = new NativeHashMap<>();
    private final transient NativeHashMap<Integer, GComponent> idComponents = new NativeHashMap<>();

    public GFormChangesDTO initialFormChanges;
    public GFormUserPreferences userPreferences;
    public HashSet<GGroupObject> inputGroupObjects;

    public GGroupObject getGroupObject(int id) {
        GGroupObject cache = idGroupObjects.get(id);
        if(cache != null)
            return cache;

        for (GGroupObject groupObject : groupObjects) {
            if (groupObject.ID == id) {
                idGroupObjects.put(id, groupObject);
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
        GObject cache = idObjects.get(id);
        if(cache != null)
            return cache;

        for (GGroupObject groupObject : groupObjects) {
            for (GObject object : groupObject.objects) {
                if (object.ID == id) {
                    idObjects.put(id, object);
                    return object;
                }
            }
        }
        return null;
    }

    public GPropertyDraw getProperty(int id) {
        GPropertyDraw cache = idProps.get(id);
        if(cache != null)
            return cache;

        for (GPropertyDraw property : propertyDraws) {
            if (property.ID == id) {
                idProps.put(id, property);
                return property;
            }
        }
        return null;
    }

    public GPropertyDraw getProperty(String propertyFormName) {
        for (GPropertyDraw property : propertyDraws) {
            if (property.propertyFormName.equals(propertyFormName)) {
                return property;
            }
        }
        return null;
    }
    
    public GContainer findContainerByID(int id) {
        GContainer cache = idContainers.get(id);
        if(cache != null)
            return cache;

        GContainer result = mainContainer.findContainerByID(id);
        idContainers.put(id, result);
        return result;
    }

    public GComponent findComponentByID(int id) {
        GComponent cache = idComponents.get(id);
        if(cache != null)
            return cache;

        GComponent result = mainContainer.findComponentByID(id);
        idComponents.put(id, result);
        return result;
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders(GGroupObject group) {
        LinkedHashMap<GPropertyDraw, Boolean> result = new LinkedHashMap<>();
        for (Map.Entry<GPropertyDraw, Boolean> entry : defaultOrders.entrySet()) {
            if (GwtSharedUtils.nullEquals(entry.getKey().groupObject, group)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> getPivotColumns(GGroupObject group) {
        return getPivotProperties(group, pivotColumns);
    }

    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> getPivotRows(GGroupObject group) {
        return getPivotProperties(group, pivotRows);
    }

    // copy of GroupObjectEntity method
    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> getPivotProperties(GGroupObject group, ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> properties) {
        ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> result = new ArrayList<>();
        for (ArrayList<GPropertyDrawOrPivotColumn> propertyEntry : properties) {
            ArrayList<GPropertyDrawOrPivotColumn> resultEntry = new ArrayList<>();
            for(GPropertyDrawOrPivotColumn property : propertyEntry) {
                if (property.equalsGroupObject(group)) {
                    resultEntry.add(property);
                }
            }
            result.add(resultEntry);
        }
        return result;
    }

    public ArrayList<GPropertyDraw> getPivotMeasures(GGroupObject group) {
        ArrayList<GPropertyDraw> result = new ArrayList<>();
        for (GPropertyDraw property : pivotMeasures) {
            if (property.equalsGroupObject(group)) {
                result.add(property);
            }
        }
        return result;
    }

    public String getCreationPath() {
        return creationPath;
    }

    public String getPath() {
        return path;
    }

    public String getTooltip() {
        String caption = mainContainer.caption;
        return MainFrame.showDetailedInfo ?
                GwtSharedUtils.stringFormat("<html><body>" +
                        "<b>%s</b><br/>" +
                        createTooltipHorizontalSeparator() +
                        "<b>sID:</b> %s<br/>" +
                        "<b>" + ClientMessages.Instance.get().tooltipPath() + ":</b> %s<a class='lsf-tooltip-path'></a> &ensp; <a class='lsf-tooltip-help'></a>" +
                        "</body></html>", caption, canonicalName, creationPath) :
                GwtSharedUtils.stringFormat("<html><body><b>%s</b></body></html>", caption);
    }
}
