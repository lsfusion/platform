package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.io.Serializable;
import java.util.Objects;

public class GPropertyFilter {
    public GGroupObject groupObject;
    public GPropertyDraw property;
    public GFilterValue value;

    public GGroupObjectValue columnKey;

    public boolean negation;
    public GCompare compare;
    public boolean junction; //true - conjunction, false - disjunction

    public GPropertyFilter(GGroupObject groupObject, GPropertyDraw property, GGroupObjectValue columnKey, Object value, GCompare compare) {
        this(groupObject, property, new GDataFilterValue((Serializable) value), columnKey, false, compare, true);
    }
    public GPropertyFilter(GGroupObject groupObject, GPropertyDraw property, GFilterValue value, GGroupObjectValue columnKey, boolean negation, GCompare compare, boolean junction) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPropertyFilter)) return false;
        GPropertyFilter that = (GPropertyFilter) o;
        return negation == that.negation &&
                junction == that.junction &&
                groupObject.equals(that.groupObject) &&
                property.equals(that.property) &&
                value.equals(that.value) &&
                columnKey.equals(that.columnKey) &&
                compare == that.compare;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupObject, property, value, columnKey, negation, compare, junction);
    }
}
