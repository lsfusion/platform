package lsfusion.server.form.entity.drilldown;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.XorUnionProperty;

public class XorUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<XorUnionProperty.Interface, XorUnionProperty> {

    public XorUnionDrillDownFormEntity(String sID, String caption, XorUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
