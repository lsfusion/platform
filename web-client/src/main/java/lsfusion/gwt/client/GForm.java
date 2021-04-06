package lsfusion.gwt.client;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontWidthString;
import lsfusion.gwt.client.form.design.GWidthStringProcessor;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;

public class GForm implements Serializable, GWidthStringProcessor {
    public String sessionID;

    public String sID;
    
    public String canonicalName;

    public String creationPath;

    public int autoRefresh;

    public GContainer mainContainer;
    public HashSet<GTreeGroup> treeGroups = new HashSet<>();
    public ArrayList<GGroupObject> groupObjects = new ArrayList<>();
    public ArrayList<GPropertyDraw> propertyDraws = new ArrayList<>();
    public ArrayList<GRegularFilterGroup> regularFilterGroups = new ArrayList<>();
    public LinkedHashMap<GPropertyDraw, Boolean> defaultOrders = new LinkedHashMap<>();

    public List<List<GPropertyDraw>> pivotColumns = new ArrayList<>();
    public List<List<GPropertyDraw>> pivotRows = new ArrayList<>();
    public List<GPropertyDraw> pivotMeasures = new ArrayList<>();

    private transient HashMap<Integer, GPropertyDraw> idProps;
    private transient HashMap<Integer, GObject> idObjects;

    public GFormChangesDTO initialFormChanges;
    public GFormUserPreferences userPreferences;
    public Set<GGroupObject> inputGroupObjects;

    public ArrayList<GFontWidthString> usedFonts = new ArrayList<>();
    
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

    public GPropertyDraw getProperty(int id) {
        GPropertyDraw prop;
        if (idProps == null) {
            idProps = new HashMap<>();
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

    public GPropertyDraw getProperty(String propertyFormName) {
        for (GPropertyDraw property : propertyDraws) {
            if (property.propertyFormName.equals(propertyFormName)) {
                return property;
            }
        }
        return null;
    }
    
    public GContainer findContainerByID(int id) {
        return mainContainer.findContainerByID(id);
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

    public List<List<GPropertyDraw>> getPivotColumns(GGroupObject group) {
        return getPivotProperties(group, pivotColumns);
    }

    public List<List<GPropertyDraw>> getPivotRows(GGroupObject group) {
        return getPivotProperties(group, pivotRows);
    }

    // copy of GroupObjectEntity method
    public List<List<GPropertyDraw>> getPivotProperties(GGroupObject group, List<List<GPropertyDraw>> properties) {
        List<List<GPropertyDraw>> result = new ArrayList<>();
        for (List<GPropertyDraw> propertyEntry : properties) {
            List<GPropertyDraw> resultEntry = new ArrayList<>();
            for(GPropertyDraw property : propertyEntry) {
                if (GwtSharedUtils.nullEquals(property.groupObject, group)) {
                    resultEntry.add(property);
                }
            }
            result.add(resultEntry);
        }
        return result;
    }

    public List<GPropertyDraw> getPivotMeasures(GGroupObject group) {
        List<GPropertyDraw> result = new ArrayList<>();
        for (GPropertyDraw property : pivotMeasures) {
            if (GwtSharedUtils.nullEquals(property.groupObject, group)) {
                result.add(property);
            }
        }
        return result;
    }

    public void addFont(GFont font) {
        addWidthString(new GFontWidthString(font));
    }
    public void addWidthString(GFontWidthString fontWidthString) {
        if (!usedFonts.contains(fontWidthString)) {
            usedFonts.add(fontWidthString);
        }
    }

    public String getCaption() {
        return mainContainer.caption;
    }

    public String getTooltip(String caption) {
        return MainFrame.showDetailedInfo ?
                GwtSharedUtils.stringFormat("<html><body>" +
                        "<b>%s</b><br/>" + 
                        createTooltipHorizontalSeparator() +
                        "<b>sID:</b> %s<br/>" +
                        "<b>" + ClientMessages.Instance.get().tooltipPath() + ":</b> %s<br/>" +
                        "</body></html>", caption, canonicalName, creationPath) :
                GwtSharedUtils.stringFormat("<html><body><b>%s</b></body></html>", caption);
    }
}
