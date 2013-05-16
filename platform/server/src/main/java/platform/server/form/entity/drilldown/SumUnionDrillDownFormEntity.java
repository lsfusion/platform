package platform.server.form.entity.drilldown;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.SumUnionProperty;

public class SumUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<SumUnionProperty.Interface, SumUnionProperty> {

    public SumUnionDrillDownFormEntity(String sID, String caption, SumUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
