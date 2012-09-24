package platform.gwt.form2.shared.view;

import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.reader.GRowBackgroundReader;
import platform.gwt.form2.shared.view.reader.GRowForegroundReader;

import java.io.Serializable;
import java.util.*;

public class GGroupObject implements Serializable {
    public List<GObject> objects = new ArrayList<GObject>();

    public GGrid grid;
    public GShowType showType;
    public GToolbar toolbar;
    public GFilter filter;
    public int ID;
    public List<String> banClassView;

    public boolean isRecursive;
    public GTreeGroup parent;
    public List<GGroupObject> upTreeGroups = new ArrayList<GGroupObject>();

    public GRowBackgroundReader rowBackgroundReader;
    public GRowForegroundReader rowForegroundReader;

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
        ArrayList<GGroupObject> result = new ArrayList<GGroupObject>(upTreeGroups);
        result.add(this);
        return result;
    }

    public boolean isLastGroupInTree() {
        boolean last = false;
        if (parent.groups.size() > 0)
            last = parent.groups.get(parent.groups.size() - 1) == this;
        return parent != null && last;
    }

    public static List<GGroupObjectValue> mergeGroupValues(LinkedHashMap<GGroupObject, List<GGroupObjectValue>> groupColumnKeys) {
        if (groupColumnKeys.isEmpty()) {
            return Arrays.asList(new GGroupObjectValue());
        } else if (groupColumnKeys.size() == 1) {
            return groupColumnKeys.values().iterator().next();
        }

        //находим декартово произведение ключей колонок
        ArrayList<GGroupObjectValue> propColumnKeys = new ArrayList<GGroupObjectValue>();
        propColumnKeys.add(new GGroupObjectValue());
        for (Map.Entry<GGroupObject, List<GGroupObjectValue>> entry : groupColumnKeys.entrySet()) {
            List<GGroupObjectValue> groupObjectKeys = entry.getValue();

            ArrayList<GGroupObjectValue> newPropColumnKeys = new ArrayList<GGroupObjectValue>();
            for (GGroupObjectValue propColumnKey : propColumnKeys) {
                for (GGroupObjectValue groupObjectKey : groupObjectKeys) {
                    newPropColumnKeys.add(new GGroupObjectValue(propColumnKey, groupObjectKey));
                }
            }
            propColumnKeys = newPropColumnKeys;
        }
        return propColumnKeys;
    }
}
