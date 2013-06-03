package lsfusion.server.form.entity.drilldown;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.SumUnionProperty;

public class SumUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<SumUnionProperty.Interface, SumUnionProperty> {

    public SumUnionDrillDownFormEntity(String sID, String caption, SumUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
