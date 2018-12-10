package lsfusion.gwt.shared.view.filter;

import lsfusion.gwt.shared.view.GGroupObject;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.shared.view.changes.dto.GPropertyFilterDTO;

public class GPropertyFilter {
    public GGroupObject groupObject;
    public GPropertyDraw property;
    public GFilterValue value;
    
    public GGroupObjectValue columnKey;

    public boolean negation;
    public GCompare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

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

    public GCompare getDefaultCompare() {
        return property.defaultCompare != null ? property.defaultCompare : property.baseType.getDefaultCompare();
    }
}
