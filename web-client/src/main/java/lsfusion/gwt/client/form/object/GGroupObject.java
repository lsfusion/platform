package lsfusion.gwt.client.form.object;

import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.property.*;

import java.io.Serializable;
import java.util.*;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public class GGroupObject implements Serializable, HasNativeSID {
    public List<GObject> objects = new ArrayList<>();

    public GContainer filtersContainer;
    public GFilterControls filtersControls;
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
    public GCustomOptionsReader customOptionsReader;

    // transient
    public transient GSize columnSumWidth = GSize.ZERO;
    public transient int columnCount;
    public transient GSize rowMaxHeight = GSize.ZERO;

    public GSize getWidth(int columns) {
        int columnCount = this.columnCount;
        if(columns == -1)
            columns = Math.min(columnCount <= 3 ? columnCount : (int) round(3 + pow(columnCount - 6, 0.7)), 6);

        return columnCount > 0 ? columnSumWidth.scale(columns).div(columnCount) : GSize.ZERO;
    }

    public GSize getHeight(int lines, GSize headerHeight) {
        if(lines == -1)
            lines = 5;

        return (rowMaxHeight.add(DataGrid.BORDER_VERT_SIZE).scale(lines)).add(
                3 * DataGrid.BORDER_VERT_SIZE).add( // borders around grid + header border
                (headerHeight != null ? headerHeight : GGridPropertyTableHeader.DEFAULT_HEADER_HEIGHT));
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

    public static ArrayList<GGroupObjectValue> mergeGroupValues(LinkedHashMap<GGroupObject, ArrayList<GGroupObjectValue>> groupObjectColumnKeys) {
        if (groupObjectColumnKeys.isEmpty()) {
            return GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        } else if (groupObjectColumnKeys.size() == 1) {
            return groupObjectColumnKeys.values().iterator().next();
        }

        //находим декартово произведение ключей колонок
        ArrayList<GGroupObjectValue> propColumnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        for (Map.Entry<GGroupObject, ArrayList<GGroupObjectValue>> entry : groupObjectColumnKeys.entrySet()) {
            ArrayList<GGroupObjectValue> groupColumnKeys = entry.getValue(); // mutable

            if(propColumnKeys.isEmpty()) {
                propColumnKeys = new ArrayList<>(groupColumnKeys);
            } else {
                ArrayList<GGroupObjectValue> newPropColumnKeys = new ArrayList<>();
                for (GGroupObjectValue propColumnKey : propColumnKeys) {
                    for (GGroupObjectValue groupObjectKey : groupColumnKeys) {
                        newPropColumnKeys.add(GGroupObjectValue.getFullKey(propColumnKey, groupObjectKey));
                    }
                }
                propColumnKeys = newPropColumnKeys;
            }
        }

        return propColumnKeys;
    }

    public GGroupObjectValue filterRowKeys(GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filter(Collections.singletonList(this));
    }
}
