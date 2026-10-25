package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.List;

public class GPropertyFilter {
    public GFilter filter;
    public GGroupObject groupObject;
    // the property in one of its columns; no column key - the column of the current objects of the column groups
    public Column column;
    public GDataFilterValue value;

    public boolean negation;
    public GCompare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public GPropertyFilter(GFilter filter, GGroupObject groupObject, GGroupObjectValue columnKey, PValue value, GCompare compare) {
        this(filter, groupObject, columnKey, value, null, compare, null);
    }
    public GPropertyFilter(GFilter filter, GGroupObject groupObject, GGroupObjectValue columnKey, PValue value, Boolean negation, GCompare compare, Boolean junction) {
        this.filter = filter;
        this.groupObject = groupObject;
        this.column = new Column(filter.property, columnKey);
        this.value = new GDataFilterValue(value);
        if (negation != null) {
            this.negation = negation;
        }
        this.compare = compare != null ? compare : filter.property.getDefaultCompare();
        if (junction != null) {
            this.junction = junction;
        }
    }

    public GPropertyFilterDTO getFilterDTO() {
        GPropertyFilterDTO filterDTO = new GPropertyFilterDTO();

        filterDTO.propertyID = column.property.ID;
        filterDTO.columnKey = column.columnKey;
        filterDTO.filterValue = value.getDTO();
        filterDTO.negation = negation;
        filterDTO.compareByte = compare.serialize();
        filterDTO.junction = junction;

        return filterDTO;
    }
    // ... and a list's, as a client sends them: an action is no filter
    public static ArrayList<GPropertyFilterDTO> getFilterDTOs(List<GPropertyFilter> filters) {
        ArrayList<GPropertyFilterDTO> filterDTOs = new ArrayList<>();
        for (GPropertyFilter filter : filters)
            if (filter.column.property.canBeFiltered())
                filterDTOs.add(filter.getFilterDTO());
        return filterDTOs;
    }
    // ... and back, for a group's whole list: the conditions its filters are, as a client sent them or the server
    // reported them - what the group's panel shows, and what a view drawing the group's FILTERS box is shown
    public static ArrayList<GPropertyFilter> getConditions(GForm form, GGroupObject group,
                                                           ArrayList<GPropertyFilterDTO> filters) {
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        for (GPropertyFilterDTO filter : filters)
            conditions.add(new GPropertyFilter(new GFilter(form.getProperty(filter.propertyID)), group,
                    filter.columnKey, PValue.convertFileValue(filter.filterValue.content), filter.negation,
                    GCompare.get(filter.compareByte), filter.junction));
        return conditions;
    }
    // a FILTERS box's list as a client states it, split into its groups' lists - a group's, or a tree's, one box for
    // all its groups (GFormController.changeFilters) -: a condition is of its property's group (groupObject), and it is
    // the box's where that group is one of the box's. One on a property of another group - or of no group - is no
    // condition of the box's groups: left out, and said on the console. Given up with it: a quick filter on a property
    // of another group (QUICKFILTER), which a grid's list carried under the grid's group - one rule for a group's box
    // and a tree's, with no mode
    public static NativeSIDMap<GGroupObject, ArrayList<GPropertyFilter>> getGroupConditions(List<GGroupObject> groups,
                                                                                          ArrayList<GPropertyFilter> conditions) {
        NativeSIDMap<GGroupObject, ArrayList<GPropertyFilter>> result = new NativeSIDMap<>();
        for (GGroupObject group : groups)
            result.put(group, new ArrayList<>());
        for (GPropertyFilter condition : conditions) {
            ArrayList<GPropertyFilter> groupConditions = condition.groupObject != null ? result.get(condition.groupObject) : null;
            if (groupConditions != null)
                groupConditions.add(condition);
            else
                GwtClientUtils.consoleError("the filters of " + getNames(groups) + ": the condition on '"
                        + condition.column.property.sID + "' is on a property "
                        + (condition.groupObject != null ? "of group '" + condition.groupObject.getSID() + "'" : "of no group")
                        + ", which is none of theirs, so it is not sent");
        }
        return result;
    }
    static String getNames(List<GGroupObject> groups) {
        StringBuilder names = new StringBuilder();
        for (GGroupObject group : groups)
            names.append(names.length() > 0 ? ", " : "").append("'").append(group.getSID()).append("'");
        return names.toString();
    }

    // ===== A VALUE GIVEN A COLUMN - a property in one of its columns, the empty key for one not grouped in columns -:
    // what it starts from, the condition on the column it goes into (GUserFiltersController.getValueBase) - its
    // comparison, negation and junction kept where whoever gives the value states none of them -, or a new one,
    // compared the property's default way
    public static GPropertyFilter getBase(Column column, GPropertyFilter first) {
        return first != null ? first : new GPropertyFilter(new GFilter(column.property), column.property.groupObject, column.columnKey, null, null);
    }
    // ... and the condition stated from it, with the value, negation, comparison and junction given: a new one, the one
    // it starts from left as it is
    public GPropertyFilter restated(PValue value, boolean negation, GCompare compare, boolean junction) {
        return new GPropertyFilter(new GFilter(column.property), groupObject, column.columnKey, value, negation, compare, junction);
    }

    public boolean nullValue() {
        return value.value == null;
    }

    public boolean isFixed() {
        return filter.fixed;
    }
}
