package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.io.Serializable;

public class GPropertyFilter {
    public GFilter filter;
    public GGroupObject groupObject;
    public GPropertyDraw property;
    public GDataFilterValue value;

    public GGroupObjectValue columnKey;

    public boolean negation;
    public GCompare compare;
    public boolean junction; //true - conjunction, false - disjunction

    public GPropertyFilter(GFilter filter, GGroupObject groupObject, GGroupObjectValue columnKey, Object value, GCompare compare) {
        this(filter, groupObject, filter.property, new GDataFilterValue((Serializable) value), columnKey, false, compare, true);
    }
    public GPropertyFilter(GFilter filter, GGroupObject groupObject, GPropertyDraw property, GDataFilterValue value, GGroupObjectValue columnKey, boolean negation, GCompare compare, boolean junction) {
        this.filter = filter;
        this.groupObject = groupObject;
        this.property = property;
        this.value = value;
        this.columnKey = columnKey;
        this.negation = negation;
        this.compare = compare;
        this.junction = junction;
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
}
