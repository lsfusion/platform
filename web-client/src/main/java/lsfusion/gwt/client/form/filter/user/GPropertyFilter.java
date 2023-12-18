package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

public class GPropertyFilter {
    public GFilter filter;
    public GGroupObject groupObject;
    public GPropertyDraw property;
    public GDataFilterValue value;

    public GGroupObjectValue columnKey;

    public boolean negation;
    public GCompare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public GPropertyFilter(GFilter filter, GGroupObject groupObject, GGroupObjectValue columnKey, PValue value, GCompare compare) {
        this(filter, groupObject, columnKey, value, null, compare, null);
    }
    public GPropertyFilter(GFilter filter, GGroupObject groupObject, GGroupObjectValue columnKey, PValue value, Boolean negation, GCompare compare, Boolean junction) {
        this.filter = filter;
        this.groupObject = groupObject;
        this.property = filter.property;
        this.value = new GDataFilterValue(value);
        this.columnKey = columnKey;
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

        filterDTO.propertyID = property.ID;
        filterDTO.columnKey = columnKey;
        filterDTO.filterValue = value.getDTO();
        filterDTO.negation = negation;
        filterDTO.compareByte = compare.serialize();
        filterDTO.junction = junction;

        return filterDTO;
    }
    
    public boolean nullValue() {
        return value.value == null;
    }

    public boolean isFixed() {
        return filter.fixed;
    }

    public boolean columnEquals(GPropertyFilter obj) {
        return property.equals(obj.property) && GwtSharedUtils.nullEquals(columnKey, obj.columnKey);
    }

    public boolean columnEquals(Pair<GPropertyDraw, GGroupObjectValue> column) {
        return property.equals(column.first) && GwtSharedUtils.nullEquals(columnKey, column.second);
    }

    public void override(GPropertyFilter filter) {
        compare = filter.compare;
        junction = filter.junction;
        negation = filter.negation;
        value.setValue(filter.value.value);
    }
}
