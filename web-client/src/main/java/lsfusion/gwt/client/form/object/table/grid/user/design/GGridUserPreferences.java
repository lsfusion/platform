package lsfusion.gwt.client.form.object.table.grid.user.design;

import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GGridUserPreferences {
    private Map<GPropertyDraw, GColumnUserPreferences> columnUserPreferences;
    public GFont font;
    public Integer pageSize;
    public Integer headerHeight;
    public GGroupObject groupObject;
    private Boolean hasUserPreferences;

    public GGridUserPreferences(GGroupObject groupObject) {
        this(groupObject, new HashMap<GPropertyDraw, GColumnUserPreferences>(), null, null, null, null);
    }

    public GGridUserPreferences(GGroupObject groupObject, Map<GPropertyDraw, GColumnUserPreferences> columnUserPreferences, GFont font, Integer pageSize,
                                Integer headerHeight, Boolean hasUserPreferences) {
        this.groupObject = groupObject;
        this.columnUserPreferences = columnUserPreferences;
        this.font = font;
        this.pageSize = pageSize;
        this.headerHeight = headerHeight;
        this.hasUserPreferences = hasUserPreferences;
    }

    public GGridUserPreferences(GGridUserPreferences prefs) {
        this(prefs.groupObject, new HashMap<GPropertyDraw, GColumnUserPreferences>(), prefs.font, prefs.pageSize, prefs.headerHeight, prefs.hasUserPreferences);

        for (Map.Entry<GPropertyDraw, GColumnUserPreferences> entry : prefs.columnUserPreferences.entrySet()) {
            columnUserPreferences.put(entry.getKey(), new GColumnUserPreferences(entry.getValue()));
        }
    }

    public boolean hasUserPreferences() {
        return hasUserPreferences != null && hasUserPreferences;
    }

    public void setHasUserPreferences(boolean hasUserPreferences) {
        this.hasUserPreferences = hasUserPreferences;
    }

    public GColumnUserPreferences getColumnPreferences(GPropertyDraw property) {
        return columnUserPreferences.get(property);
    }
    
    public Map<GPropertyDraw, GColumnUserPreferences> getColumnUserPreferences() {
        return columnUserPreferences;
    }

    public Boolean getUserHide(GPropertyDraw property) {
        return ensureColumnPreferences(property).userHide;
    }

    public String getUserCaption(GPropertyDraw property) {
        return ensureColumnPreferences(property).userCaption;
    }

    public String getUserPattern(GPropertyDraw property) {
        return ensureColumnPreferences(property).userPattern;
    }

    public Integer getUserWidth(GPropertyDraw property) {
        return ensureColumnPreferences(property).userWidth;
    }

    public Double getUserFlex(GPropertyDraw property) {
        return ensureColumnPreferences(property).userFlex;
    }

    public Integer getUserOrder(GPropertyDraw property) {
        return ensureColumnPreferences(property).userOrder;
    }

    public Integer getUserSort(GPropertyDraw property) {
        return ensureColumnPreferences(property).userSort;
    }

    public Boolean getUserAscendingSort(GPropertyDraw property) {
        return ensureColumnPreferences(property).userAscendingSort;
    }

    private GColumnUserPreferences ensureColumnPreferences(GPropertyDraw property) {
        GColumnUserPreferences prefs = columnUserPreferences.get(property);
        if (prefs == null) {
            prefs = new GColumnUserPreferences(null, null, null, null, null, null, null, null);
            columnUserPreferences.put(property, prefs);
        }
        return prefs;
    }

    public void setUserHide(GPropertyDraw property, Boolean userHide) {
        ensureColumnPreferences(property).userHide = userHide;
    }

    public void setColumnSettings(GPropertyDraw property, String userCaption, String userPattern, Integer userOrder, Boolean userHide) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userCaption = userCaption;
        prefs.userPattern = userPattern;
        prefs.userOrder = userOrder;
        prefs.userHide = userHide;
    }

    public void setUserWidth(GPropertyDraw property, Integer userWidth) {
        ensureColumnPreferences(property).userWidth = userWidth;
    }

    public void setUserFlex(GPropertyDraw property, Double userFlex) {
        ensureColumnPreferences(property).userFlex = userFlex;
    }

    public void setUserOrder(GPropertyDraw property, Integer userOrder) {
        ensureColumnPreferences(property).userOrder = userOrder;
    }

    public void setUserSort(GPropertyDraw property, Integer userSort) {
        ensureColumnPreferences(property).userSort = userSort;
    }

    public void setUserAscendingSort(GPropertyDraw property, Boolean userAscendingSort) {
        ensureColumnPreferences(property).userAscendingSort = userAscendingSort;
    }

    public void setUserOrder(GPropertyDraw property, Boolean userAscendingSort) {
        ensureColumnPreferences(property).userAscendingSort = userAscendingSort;
    }

    public void resetPreferences(GPropertyDraw property) {
        columnUserPreferences.put(property, new GColumnUserPreferences(null, null, null, null, null, null, null, null));
    }

    public void resetPreferences() {
        font = new GFont(null, -1, false, false);
        pageSize = null;
        headerHeight = null;
        hasUserPreferences = false;
        for (GPropertyDraw property : new HashSet<>(columnUserPreferences.keySet())) {
            columnUserPreferences.put(property, new GColumnUserPreferences(null, null, null, null, null, null, null, null));
        }
    }

    public Comparator<GPropertyDraw> getUserSortComparator() {
        return  new Comparator<GPropertyDraw>() {
            public int compare(GPropertyDraw c1, GPropertyDraw c2) {
                if (getUserAscendingSort(c1) != null && getUserAscendingSort(c2) != null) {
                    return getUserSort(c1) - getUserSort(c2);
                } else {
                    return 0;
                }
            }
        };
    }

    public Comparator<GPropertyDraw> getUserOrderComparator() {
        return new Comparator<GPropertyDraw>() {
            public int compare(GPropertyDraw c1, GPropertyDraw c2) {
                if (getUserOrder(c1) == null)
                    return getUserOrder(c2) == null ? 0 : 1;
                else
                    return getUserOrder(c2) == null ? -1 : (getUserOrder(c1) - getUserOrder(c2));
            }
        };
    }

    public GGroupObjectUserPreferences convertPreferences() {
        Map<String, GColumnUserPreferences> columns = new HashMap<>();
        for (Map.Entry<GPropertyDraw, GColumnUserPreferences> entry : columnUserPreferences.entrySet()) {
            columns.put(entry.getKey().propertyFormName, entry.getValue());
        }
        return new GGroupObjectUserPreferences(columns, groupObject.getSID(), font, pageSize, headerHeight, hasUserPreferences());
    }
}
