package lsfusion.server.form.entity.drilldown;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.MaxUnionProperty;
import lsfusion.server.logics.property.PropertyInterface;

public class MaxUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<MaxUnionProperty.Interface, MaxUnionProperty> {

    public MaxUnionDrillDownFormEntity(String sID, String caption, MaxUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
