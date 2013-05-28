package platform.server.form.entity.drilldown;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.MaxUnionProperty;
import platform.server.logics.property.PropertyInterface;

public class MaxUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<MaxUnionProperty.Interface, MaxUnionProperty> {

    public MaxUnionDrillDownFormEntity(String sID, String caption, MaxUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
