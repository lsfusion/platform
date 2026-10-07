package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;

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
    
    public boolean nullValue() {
        return value.value == null;
    }

    public boolean isFixed() {
        return filter.fixed;
    }

    public void override(GPropertyFilter filter) {
        compare = filter.compare;
        junction = filter.junction;
        negation = filter.negation;
        value.setValue(filter.value.value);
    }
}
