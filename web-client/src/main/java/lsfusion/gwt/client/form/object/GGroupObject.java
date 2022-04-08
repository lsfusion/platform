package lsfusion.gwt.client.form.object;

import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.property.*;

import java.io.Serializable;
import java.util.*;

import static java.lang.Math.*;

public class GGroupObject implements Serializable, HasNativeSID {
    public List<GObject> objects = new ArrayList<>();

    public GContainer filtersContainer;
    public List<GFilter> filters = new ArrayList<>();
    
    public GGrid grid;
    public GToolbar toolbar;

    public int ID;
    public String nativeSID;
    public String sID;

    public GClassViewType viewType;
    public GListViewType listViewType;
    public GPivotOptions pivotOptions;
    public String customRenderFunction;
    public String mapTileProvider;

    public boolean asyncInit;

    public boolean isRecursive;
    public GTreeGroup parent;
    public List<GGroupObject> upTreeGroups = new ArrayList<>();

    public boolean isMap;
    public boolean isCalendarDate;
    public boolean isCalendarDateTime;
    public boolean isCalendarPeriod;

    public boolean hasHeaders;
    public boolean hasFooters;

    public GRowBackgroundReader rowBackgroundReader;
    public GRowForegroundReader rowForegroundReader;

    // transient
    public int columnSumWidth;
    public int columnCount;
    public int rowMaxHeight;

    public int getWidth(int lines) {
        int columnCount = this.columnCount;
        if(lines == -1)
            lines = Math.min(columnCount <= 3 ? columnCount : (int) round(3 + pow(columnCount - 6, 0.7)), 6);

        return columnCount > 0 ? lines * columnSumWidth / columnCount : 0;
    }

    public int getHeight(int lines, int headerHeight) {
        if(lines == -1)
            lines = 5;

        return (lines * (rowMaxHeight + DataGrid.BORDER_VERT_SIZE)) +
                + 3 * DataGrid.BORDER_VERT_SIZE // borders around grid + header border
                + (headerHeight >= 0 ? headerHeight : GGridPropertyTableHeader.DEFAULT_HEADER_HEIGHT);
    }

    public String getCaption() {
        if (objects.isEmpty()) {
            //todo: локализовать попозже через GWT-шный Messages interface
            return "Empty group";
        }

        String result = "";
        for (GObject object : objects) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += object.getCaption();
        }
        return result;
    }

    public String getSID() {
        return sID;
    }

    @Override
    public String getNativeSID() {
        return nativeSID;
    }

    public boolean mayHaveChildren() {
        return isRecursive || (parent != null && parent.groups.indexOf(this) != parent.groups.size() - 1);
    }

    public GGroupObject getUpTreeGroup() {
        if (upTreeGroups.size() > 0)
            return upTreeGroups.get(upTreeGroups.size() - 1);
        else
            return null;
    }

    public List<GGroupObject> getUpTreeGroups() {
        ArrayList<GGroupObject> result = new ArrayList<>(upTreeGroups);
        result.add(this);
        return result;
    }

    public boolean isLastGroupInTree() {
        boolean last = false;
        if (parent.groups.size() > 0)
            last = parent.groups.get(parent.groups.size() - 1) == this;
        return parent != null && last;
    }

    public static ArrayList<GGroupObjectValue> mergeGroupValues(LinkedHashMap<GGroupObject, ArrayList<GGroupObjectValue>> groupColumnKeys) {
        if (groupColumnKeys.isEmpty()) {
            return GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        } else if (groupColumnKeys.size() == 1) {
            return groupColumnKeys.values().iterator().next();
        }

        //находим декартово произведение ключей колонок
        ArrayList<GGroupObjectValueBuilder> propColumnKeys = new ArrayList<>();
        propColumnKeys.add(new GGroupObjectValueBuilder());
        for (Map.Entry<GGroupObject, ArrayList<GGroupObjectValue>> entry : groupColumnKeys.entrySet()) {
            List<GGroupObjectValue> groupObjectKeys = entry.getValue();

            ArrayList<GGroupObjectValueBuilder> newPropColumnKeys = new ArrayList<>();
            for (GGroupObjectValueBuilder propColumnKey : propColumnKeys) {
                for (GGroupObjectValue groupObjectKey : groupObjectKeys) {
                    newPropColumnKeys.add(new GGroupObjectValueBuilder(propColumnKey).putAll(groupObjectKey));
                }
            }
            propColumnKeys = newPropColumnKeys;
        }

        ArrayList<GGroupObjectValue> result = new ArrayList<>();
        for (GGroupObjectValueBuilder builder : propColumnKeys) {
            result.add(builder.toGroupObjectValue());
        }

        return result;
    }

    public GGroupObjectValue filterRowKeys(GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filter(Collections.singletonList(this));
    }
}
