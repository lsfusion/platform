package platform.server.form.entity.drilldown;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.XorUnionProperty;

public class XorUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<XorUnionProperty.Interface, XorUnionProperty> {

    public XorUnionDrillDownFormEntity(String sID, String caption, XorUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
