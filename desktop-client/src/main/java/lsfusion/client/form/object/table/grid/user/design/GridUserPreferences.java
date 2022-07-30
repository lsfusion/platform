package lsfusion.client.form.object.table.grid.user.design;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GridUserPreferences {
    public Map<ClientPropertyDraw, ColumnUserPreferences> columnUserPreferences;
    public FontInfo fontInfo;
    public Integer pageSize;
    public Integer headerHeight;
    public ClientGroupObject groupObject;
    private Boolean hasUserPreferences;
    
    public GridUserPreferences(ClientGroupObject groupObject) {
        this(groupObject, new HashMap<>(), null, null, null, null);
    }
    
    public GridUserPreferences(ClientGroupObject groupObject, Map<ClientPropertyDraw, ColumnUserPreferences> columnUserPreferences,
                               FontInfo fontInfo, Integer pageSize, Integer headerHeight, Boolean hasUserPreferences) {
        this.groupObject = groupObject;
        this.columnUserPreferences = columnUserPreferences;
        this.fontInfo = fontInfo;
        this.pageSize = pageSize;
        this.headerHeight = headerHeight;
        this.hasUserPreferences = hasUserPreferences;
    }
    
    public GridUserPreferences(GridUserPreferences prefs) {
        this(prefs.groupObject, new HashMap<>(), prefs.fontInfo, prefs.pageSize, prefs.headerHeight, prefs.hasUserPreferences);
        
        for (Map.Entry<ClientPropertyDraw, ColumnUserPreferences> entry : prefs.columnUserPreferences.entrySet()) {
            columnUserPreferences.put(entry.getKey(), new ColumnUserPreferences(entry.getValue()));
        }
    }
    
    public boolean hasUserPreferences() {
        return hasUserPreferences != null && hasUserPreferences;   
    }
    
    public void setHasUserPreferences(boolean hasUserPreferences) {
        this.hasUserPreferences = hasUserPreferences;
    }
    
    public Boolean getUserHide(ClientPropertyDraw property) {
        return ensureColumnPreferences(property).userHide;
    }

    public String getUserCaption(ClientPropertyDraw property) {
        return ensureColumnPreferences(property).userCaption;
    }

    public String getUserPattern(ClientPropertyDraw property) {
        return ensureColumnPreferences(property).userPattern;
    }
    
    public Integer getUserWidth(ClientPropertyDraw property) {
        return ensureColumnPreferences(property).userWidth;
    }
    
    public Integer getUserOrder(ClientPropertyDraw property) {
        return ensureColumnPreferences(property).userOrder;
    }
    
    public Integer getUserSort(ClientPropertyDraw property) {
        return ensureColumnPreferences(property).userSort;
    }
    
    public Boolean getUserAscendingSort(ClientPropertyDraw property) {
        return ensureColumnPreferences(property).userAscendingSort;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getHeaderHeight() {
        return headerHeight;
    }
    
    private ColumnUserPreferences ensureColumnPreferences(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = columnUserPreferences.get(property);
        if (prefs == null) {
            prefs = new ColumnUserPreferences(null, null, null, null, null, null, null, null);
            columnUserPreferences.put(property, prefs);
        }
        return prefs;
    }
    
    public void setUserHide(ClientPropertyDraw property, Boolean userHide) {
        ensureColumnPreferences(property).userHide = userHide;
    }

    public void setUserColumnsSettings(ClientPropertyDraw property, String userCaption, String userPattern, Integer userOrder, Boolean userHide) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userCaption = userCaption;
        prefs.userPattern = userPattern;
        prefs.userOrder = userOrder;
        prefs.userHide = userHide;
    }
    
    public void setUserWidth(ClientPropertyDraw property, Integer userWidth) {
        ensureColumnPreferences(property).userWidth = userWidth;
    }

    public void setUserOrder(ClientPropertyDraw property, Integer userOrder) {
        ensureColumnPreferences(property).userOrder = userOrder;    
    }

    public void setUserSort(ClientPropertyDraw property, Integer userSort) {
        ensureColumnPreferences(property).userSort = userSort;
    }

    public void setUserAscendingSort(ClientPropertyDraw property, Boolean userAscendingSort) {
        ensureColumnPreferences(property).userAscendingSort = userAscendingSort;
    }
    
    public void resetPreferences() {
        fontInfo = new FontInfo(null, -1, false, false);
        pageSize = null;
        headerHeight = null;
        hasUserPreferences = false;
        for (ClientPropertyDraw property : new HashSet<>(columnUserPreferences.keySet())) {
            columnUserPreferences.put(property, new ColumnUserPreferences(null, null, null, null, null, null, null, null));
        }
    }

    public Comparator<ClientPropertyDraw> getUserSortComparator() {
        return  new Comparator<ClientPropertyDraw>() {
            public int compare(ClientPropertyDraw c1, ClientPropertyDraw c2) {
                if(getUserAscendingSort(c1) == null) {
                    return getUserAscendingSort(c2) == null ? 0 : -1;
                } else {
                    return getUserAscendingSort(c2) == null ? 1 : (getUserSort(c1) - getUserSort(c2));
                }
            }
        };
    }
    
    public Comparator<ClientPropertyDraw> getUserOrderComparator() {
        return new Comparator<ClientPropertyDraw>() {
            public int compare(ClientPropertyDraw c1, ClientPropertyDraw c2) {
                Integer order1 = getUserOrder(c1);
                Integer order2 = getUserOrder(c2);
                if (order1 == null)
                    return order2 == null ? 0 : 1;
                else
                    return order2 == null ? -1 : (order1 - order2);
            }
        };
    }
    
    public GroupObjectUserPreferences convertPreferences() {
        Map<String, ColumnUserPreferences> columns = new HashMap<>();
        for (Map.Entry<ClientPropertyDraw, ColumnUserPreferences> entry : columnUserPreferences.entrySet()) {
            columns.put(entry.getKey().getPropertyFormName(), entry.getValue());
        }
        return new GroupObjectUserPreferences(columns, groupObject.getSID(), fontInfo, pageSize, headerHeight, hasUserPreferences());
    }
}
