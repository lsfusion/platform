package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import lsfusion.gwt.form.shared.view.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GGridUserPreferences {
    private Map<GPropertyDraw, GColumnUserPreferences> columnUserPreferences;
    public GFont font;
    public GGroupObject groupObject;
    private Boolean hasUserPreferences;

    public GGridUserPreferences(GGroupObject groupObject) {
        this(groupObject, new HashMap<GPropertyDraw, GColumnUserPreferences>(), null, null);
    }

    public GGridUserPreferences(GGroupObject groupObject, Map<GPropertyDraw, GColumnUserPreferences> columnUserPreferences, GFont font, Boolean hasUserPreferences) {
        this.groupObject = groupObject;
        this.columnUserPreferences = columnUserPreferences;
        this.font = font;
        this.hasUserPreferences = hasUserPreferences;
    }

    public GGridUserPreferences(GGridUserPreferences prefs) {
        this(prefs.groupObject, new HashMap<GPropertyDraw, GColumnUserPreferences>(), prefs.font, prefs.hasUserPreferences);

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

    public Boolean getUserHide(GPropertyDraw property) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userHide;
    }

    public String getUserCaption(GPropertyDraw property) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userCaption;
    }
    
    public Integer getUserWidth(GPropertyDraw property) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userWidth;
    }

    public Integer getUserOrder(GPropertyDraw property) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userOrder;
    }

    public Integer getUserSort(GPropertyDraw property) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userSort;
    }

    public Boolean getUserAscendingSort(GPropertyDraw property) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userAscendingSort;
    }

    private GColumnUserPreferences ensureColumnPreferences(GPropertyDraw property) {
        GColumnUserPreferences prefs = columnUserPreferences.get(property);
        if (prefs == null) {
            prefs = new GColumnUserPreferences(null, null, null, null, null, null);
            columnUserPreferences.put(property, prefs);
        }
        return prefs;
    }

    public void setUserHide(GPropertyDraw property, Boolean userHide) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userHide = userHide;
    }

    public void setUserCaption(GPropertyDraw property, String userCaption) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userCaption = userCaption;
    }
    
    public void setUserWidth(GPropertyDraw property, Integer userWidth) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userWidth = userWidth;
    }

    public void setUserOrder(GPropertyDraw property, Integer userOrder) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userOrder = userOrder;
    }

    public void setUserSort(GPropertyDraw property, Integer userSort) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userSort = userSort;
    }

    public void setUserAscendingSort(GPropertyDraw property, Boolean userAscendingSort) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userAscendingSort = userAscendingSort;
    }

    public void setUserOrder(GPropertyDraw property, Boolean userAscendingSort) {
        GColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userAscendingSort = userAscendingSort;
    }

    public void resetPreferences(GPropertyDraw property) {
        columnUserPreferences.put(property, new GColumnUserPreferences(null, null, null, null, null, null));
    }

    public void resetPreferences() {
        font = new GFont(null, -1, false, false);
        hasUserPreferences = false;
        for (GPropertyDraw property : new HashSet<GPropertyDraw>(columnUserPreferences.keySet())) {
            columnUserPreferences.put(property, new GColumnUserPreferences(null, null, null, null, null, null));
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
        Map<String, GColumnUserPreferences> columns = new HashMap<String, GColumnUserPreferences>();
        for (Map.Entry<GPropertyDraw, GColumnUserPreferences> entry : columnUserPreferences.entrySet()) {
            columns.put(entry.getKey().sID, entry.getValue());
        }
        return new GGroupObjectUserPreferences(columns, groupObject.getSID(), font, hasUserPreferences());
    }
}
